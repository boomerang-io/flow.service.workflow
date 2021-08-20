package io.boomerang.service.crud;

import static java.util.stream.Collectors.groupingBy;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.model.Approval;
import io.boomerang.model.Execution;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.InsightsSummary;
import io.boomerang.model.ListActivityResponse;
import io.boomerang.model.Sort;
import io.boomerang.model.Task;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.TaskOutputResult;
import io.boomerang.model.TeamWorkflowSummary;
import io.boomerang.model.controller.TaskResult;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.FlowTeamEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.FlowTriggerEnum;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskTemplateConfig;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowTeamService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.FlowApprovalService;
import io.boomerang.service.PropertyManager;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.refactor.ControllerRequestProperties;
import io.boomerang.service.runner.misc.ControllerClient;
import io.boomerang.util.DateUtil;

@Service
public class FlowActivityServiceImpl implements FlowActivityService {

  @Autowired
  private FlowWorkflowActivityService flowActivityService;

  @Autowired
  private ActivityTaskService taskService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private RevisionService versionService;

  @Autowired
  private FlowWorkflowService workflowService;

  @Autowired
  private FlowTeamService flowTeamService;

  @Value("${controller.rest.url.base}")
  private String controllerBaseUrl;

  @Value("${controller.rest.url.streamlogs}")
  private String getStreamDownloadPath;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  @Autowired
  private FlowApprovalService approvalService;

  @Autowired
  private FlowWorkflowActivityService workflowActivityService;

  @Autowired
  private TeamService teamService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private PropertyManager propertyManager;

  @Autowired
  @Lazy
  private ControllerClient controllerClient;


  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private RevisionService revisionService;

  private static final Logger LOGGER = LogManager.getLogger();

  private List<FlowActivity> convert(List<ActivityEntity> records) {

    final List<FlowActivity> flowActivities = new LinkedList<>();

    for (final ActivityEntity record : records) {
      final FlowActivity flow = new FlowActivity(record);
      final WorkflowEntity workflow = workflowService.getWorkflow(record.getWorkflowId());

      if (workflow != null) {
        flow.setWorkflowName(workflow.getName());
        flow.setDescription(workflow.getDescription());
        flow.setIcon(workflow.getIcon());
        flow.setShortDescription(workflow.getShortDescription());
      }

      flowActivities.add(flow);
    }
    return flowActivities;
  }

  @Override
  public ActivityEntity createFlowActivity(String workflowVersionId, Optional<String> trigger,
      FlowExecutionRequest request, Optional<List<TaskWorkspace>> taskWorkspaces,
      List<KeyValuePair> labels) {
    final RevisionEntity entity = versionService.getWorkflowlWithId(workflowVersionId);
    WorkflowEntity workflow = workflowService.getWorkflow(entity.getWorkFlowId());

    final FlowActivity activity = new FlowActivity();
    activity.setWorkflowRevisionid(workflowVersionId);
    activity.setWorkflowId(entity.getWorkFlowId());

    if (workflow.getScope() == WorkflowScope.team) {
      activity.setTeamId(workflow.getFlowTeamId());
    } else if (workflow.getScope() == WorkflowScope.user) {
      activity.setUserId(workflow.getOwnerUserId());
    }

    activity.setScope(workflow.getScope());
    activity.setCreationDate(new Date());
    activity.setStatus(TaskStatus.inProgress);

    List<KeyValuePair> corePropertyList = new LinkedList<>();
    if (labels != null) {
      corePropertyList.addAll(labels);
    }
    if (workflow.getLabels() != null) {
      corePropertyList.addAll(workflow.getLabels());
    }
    activity.setLabels(labels);

    if (taskWorkspaces.isPresent()) {
      activity.setTaskWorkspaces(taskWorkspaces.get());
    }

    if (trigger.isPresent()) {
      activity.setTrigger(trigger.get());
    }

    if (!trigger.isPresent() || "manual".equals(trigger.get())) {
      final FlowUserEntity userEntity = userIdentityService.getCurrentUser();
      activity.setInitiatedByUserId(userEntity.getId());
      activity.setInitiatedByUserName(userEntity.getName());
      activity.setTrigger(FlowTriggerEnum.manual.toString());
    }

    if (request.getProperties() != null) {
      Map<String, String> properties = request.getProperties();
      List<KeyValuePair> propertyList = new LinkedList<>();
      for (Map.Entry<String, String> entry : properties.entrySet()) {
        String key = entry.getKey();
        String value = properties.get(key);
        KeyValuePair prop = new KeyValuePair();
        prop.setKey(key);
        prop.setValue(value);
        propertyList.add(prop);
      }
      activity.setProperties(propertyList);
    }
    return flowActivityService.saveWorkflowActivity(activity);
  }

  @Override
  public ActivityEntity findWorkflowActivity(String id) {
    return flowActivityService.findWorkflowActivtyById(id);
  }

  @Override
  public ListActivityResponse getAllActivites(Optional<Date> from, Optional<Date> to, Pageable page,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds,
      Optional<List<String>> statuses, Optional<List<String>> triggers,
      Optional<List<String>> scopes, String property, Direction direction) {

    System.out.println("Activites");
    
    final FlowUserEntity user = userIdentityService.getCurrentUser();
    List<String> workflowIdsList = new LinkedList<>();

    if (scopes.isPresent() && !scopes.get().isEmpty()) {
      
      List<String> scopeList = scopes.get();
      if (scopeList.contains("user")) {        
        addUserWorkflows(user, workflowIdsList);
      }
      if (scopeList.contains("system") && user.getType() == UserType.admin) {
        addSystemWorkflows(workflowIdsList);
      }
      if (scopeList.contains("team")) {
        addTeamWorkflows(user, workflowIdsList, teamIds);
      }
    } else {
      
      System.out.println("No filters");
      
      addUserWorkflows(user, workflowIdsList);
      addTeamWorkflows(user, workflowIdsList, teamIds);
      if (user.getType() == UserType.admin) {
        addSystemWorkflows(workflowIdsList);
      }
    }

    ListActivityResponse response = new ListActivityResponse();
    Page<ActivityEntity> records = flowActivityService.getAllActivites(from, to, page,
        Optional.of(workflowIdsList), statuses, triggers);
    final List<FlowActivity> activities = convert(records.getContent());
    List<FlowActivity> activitiesFiltered = new ArrayList<>();
    for (FlowActivity activity : activities) {
      String workFlowId = activity.getWorkflowId();
      addTeamInformation(teamIds, activitiesFiltered, activity, workFlowId);
    }

    io.boomerang.model.Pageable pageablefinal = createPageable(records, property, direction,
        activitiesFiltered, activitiesFiltered.size());
    response.setPageable(pageablefinal);
    response.setRecords(activities);
       
    return response;
  }

  private void addTeamWorkflows(final FlowUserEntity user, List<String> workflowIdsList,
      Optional<List<String>> teamIds) {

    if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
      List<WorkflowEntity> allTeamWorkflows = this.workflowService.getWorkflowsForTeams(teamIds.get());
      List<String> allTeamWorkflowsIds =
          allTeamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
      workflowIdsList.addAll(allTeamWorkflowsIds);
    } else {
      if (user.getType() == UserType.admin) {
        List<WorkflowEntity> allTeamWorkflows = this.workflowService.getTeamWorkflows();
        List<String> workflowIds =
            allTeamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
        workflowIdsList.addAll(workflowIds);
      } else {
        List<FlowTeamEntity> flowTeam = teamService.getUsersTeamListing(user);
        List<String> flowTeamIds =
            flowTeam.stream().map(FlowTeamEntity::getId).collect(Collectors.toList());
        List<WorkflowEntity> teamWorkflows = this.workflowService.getWorkflowsForTeams(flowTeamIds);
        List<String> allTeamWorkflowsIds =
            teamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
        workflowIdsList.addAll(allTeamWorkflowsIds);
      }
    }
  }

  private void addSystemWorkflows(List<String> workflowIdsList) {
    List<WorkflowEntity> systemWorkflows = this.workflowService.getSystemWorkflows();
    List<String> systemWorkflowsIds =
        systemWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(systemWorkflowsIds);
  }

  private void addUserWorkflows(final FlowUserEntity user, List<String> workflowIdsList) {
    String userId = user.getId();
    List<WorkflowEntity> userWorkflows = this.workflowService.getUserWorkflows(userId);
    List<String> userWorkflowIds =
        userWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    System.out.println(userWorkflowIds.size());
    
    workflowIdsList.addAll(userWorkflowIds);
  }

  protected io.boomerang.model.Pageable createPageable(final Page<ActivityEntity> records,
      String property, Direction direction, List<FlowActivity> activities, int totalElements) {
    io.boomerang.model.Pageable pageable = new io.boomerang.model.Pageable();

    pageable.setNumberOfElements(records.getNumberOfElements());
    pageable.setNumber(records.getNumber());
    pageable.setSize(records.getSize());
    pageable.setTotalElements(records.getTotalElements());

    pageable.setTotalPages(records.getTotalPages());
    pageable.setFirst(records.isFirst());
    pageable.setLast(records.isLast());

    List<Sort> listSort = new ArrayList<>();
    Sort sort = new Sort();
    sort.setDirection(direction);
    sort.setProperty(property);
    listSort.add(sort);
    pageable.setSort(listSort);

    return pageable;
  }


  protected io.boomerang.model.Pageable createPageable(final Page<ActivityEntity> records,
      String property, Direction direction) {
    io.boomerang.model.Pageable pageable = new io.boomerang.model.Pageable();

    pageable.setNumberOfElements(records.getNumberOfElements());
    pageable.setNumber(records.getNumber());
    pageable.setSize(records.getSize());
    pageable.setTotalElements(records.getTotalElements());

    pageable.setTotalPages(records.getTotalPages());
    pageable.setFirst(records.isFirst());
    pageable.setLast(records.isLast());

    List<Sort> listSort = new ArrayList<>();
    Sort sort = new Sort();
    sort.setDirection(direction);
    sort.setProperty(property);
    listSort.add(sort);
    pageable.setSort(listSort);

    return pageable;
  }

  @Override
  public Map<String, Long> getActivitySummary(Pageable pageable, List<String> teamIds,
      List<String> triggers, Long fromDate, Long toDate) {
    Optional<Date> to =
        toDate == null ? Optional.empty() : Optional.of(DateUtil.asDate(getDateTime(toDate)));
    Optional<Date> from =
        fromDate == null ? Optional.empty() : Optional.of(DateUtil.asDate(getDateTime(fromDate)));

    List<String> workflowIds = new ArrayList<>();
    if (teamIds != null && !teamIds.isEmpty()) {
      workflowIds = workflowService.getWorkflowsForTeams(teamIds).stream()
          .map(WorkflowEntity::getId).collect(Collectors.toList());
    }

    List<ActivityEntity> flowWorkflowActivityEntities =
        flowActivityService.getAllActivites(from, to, pageable, getOptional(workflowIds),
            Optional.empty(), getOptional(triggers)).getContent();
    Map<String, Long> result = flowWorkflowActivityEntities.stream()
        .collect(groupingBy(v -> getStatusValue(v), Collectors.counting())); // NOSONAR
    result.put("all", Long.valueOf(flowWorkflowActivityEntities.size()));

    Arrays.stream(TaskStatus.values()).forEach(v -> initializeValue(v.getStatus(), result));
    return result;
  }

  private LocalDateTime getDateTime(Long toDate) {
    return Instant.ofEpochSecond(toDate.longValue()).atZone(ZoneId.systemDefault())
        .toLocalDateTime();
  }

  private <T> Optional<List<T>> getOptional(List<T> list) {
    if (list == null || list.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(list);
  }

  private String getStatusValue(ActivityEntity v) {
    return v.getStatus() == null ? "no_status" : v.getStatus().getStatus();
  }

  private void initializeValue(String key, Map<String, Long> result) {
    if (!result.containsKey(key)) {
      result.put(key, Long.valueOf(0));
    }
  }

  private void addTeamInformation(Optional<List<String>> teamIds,
      List<FlowActivity> activitiesFiltered, FlowActivity activity, String workFlowId) {
    String teamId = null;

    WorkflowEntity workflow = workflowService.getWorkflow(workFlowId);
    if (workflow != null) {
      if (workflow.getScope().equals((WorkflowScope.system))) {
        activitiesFiltered.add(activity);
      } else if ((workflow.getScope().equals(WorkflowScope.team))) {
        teamId = workflowService.getWorkflow(workFlowId).getFlowTeamId();
      }
    }

    if (teamId != null) {

      FlowTeamEntity team = teamService.getTeamById(teamId);
      if (team != null) {
        String teamName = team.getName();

        activity.setTeamName(teamName);

        if (teamIds.isPresent()) {
          for (String teamID : teamIds.get()) {
            if (teamId.equals(teamID)) {
              activitiesFiltered.add(activity);
            }
          }
        } else {
          activitiesFiltered.add(activity);
        }
      }
    }
  }

  @Override
  public ListActivityResponse getAllActivitesForUser(FlowUserEntity user, Optional<Date> from,
      Optional<Date> to, Pageable page, String property, Direction direction) {

    final Page<ActivityEntity> records = flowActivityService.findAllActivities(from, to, page);
    final ListActivityResponse response = new ListActivityResponse();

    final List<FlowActivity> activities = convert(records.getContent());
    io.boomerang.model.Pageable pageable = createPageable(records, property, direction);
    response.setPageable(pageable);
    response.setRecords(activities);

    return response;
  }

  protected io.boomerang.model.Pageable createPageable(final Page<ActivityEntity> records,
      String property, Direction direction, List<FlowActivity> activities,
      List<FlowActivity> totalElements) {
    io.boomerang.model.Pageable pageable = new io.boomerang.model.Pageable();

    pageable.setNumberOfElements(
        records.getSize() * records.getNumber() + 1 <= activities.size() ? totalElements.size()
            : totalElements.size() % records.getSize());
    pageable.setNumber(records.getNumber());
    pageable.setSize(records.getSize());
    pageable.setTotalElements((long) totalElements.size());

    pageable.setTotalPages((int) Math
        .ceil((Double.valueOf(totalElements.size()) / Double.valueOf(records.getSize()))));
    pageable.setFirst(records.isFirst());
    pageable.setLast(records.isLast());

    List<Sort> listSort = new ArrayList<>();
    Sort sort = new Sort();
    sort.setDirection(direction);
    sort.setProperty(property);
    listSort.add(sort);
    pageable.setSort(listSort);

    return pageable;
  }

  @Override
  public List<TaskExecutionResponse> getTaskExecutions(String activityId) {


    List<TaskExecutionEntity> activites = taskService.findTaskActiivtyForActivity(activityId);
    List<TaskExecutionResponse> taskExecutionResponses = new LinkedList<>();

    for (TaskExecutionEntity task : activites) {
      TaskExecutionResponse response = new TaskExecutionResponse();
      BeanUtils.copyProperties(task, response);

      if (TaskType.approval.equals(task.getTaskType())
          || TaskType.manual.equals(task.getTaskType())) {
        Approval approval = approvalService.getApprovalByTaskActivityId(task.getId());
        response.setApproval(approval);
      } else if (TaskType.runworkflow == task.getTaskType()
          && task.getRunWorkflowActivityId() != null) {

        String runWorkflowActivityId = task.getRunWorkflowActivityId();
        ActivityEntity activity =
            this.flowActivityService.findWorkflowActivtyById(runWorkflowActivityId);
        if (activity != null) {
          response.setRunWorkflowActivityStatus(activity.getStatus());
        }
      } else if (TaskType.eventwait == task.getTaskType()) {
        List<TaskOutputResult> results = new LinkedList<>();
        TaskOutputResult result = new TaskOutputResult();
        result.setName("eventPayload");
        result.setDescription("Payload that was received with the Wait For Event");
        String json = task.getOutputs().get("eventPayload");
        result.setValue(json);
        results.add(result);
        response.setResults(results);
      } else if (TaskType.template == task.getTaskType()
          || TaskType.customtask == task.getTaskType() || TaskType.script == task.getTaskType()) {
        List<TaskOutputResult> results = new LinkedList<>();
        setupTaskOutputResults(task, response, results);

      }
      taskExecutionResponses.add(response);

    }

    return taskExecutionResponses;
  }

  private void setupTaskOutputResults(TaskExecutionEntity task, TaskExecutionResponse response,
      List<TaskOutputResult> results) {

    if (task.getTemplateId() == null) {
      return;
    }

    Integer templateVersion = task.getTemplateRevision();
    FlowTaskTemplateEntity flowTaskTemplate =
        templateService.getTaskTemplateWithId(task.getTemplateId());

    String templateType = flowTaskTemplate.getNodetype();

    if ("templateTask".equals(templateType)) {
      List<Revision> revisions = flowTaskTemplate.getRevisions();
      if (revisions != null) {
        Optional<Revision> result = revisions.stream().parallel()
            .filter(revision -> revision.getVersion().equals(templateVersion)).findAny();
        if (result.isPresent()) {
          Revision revision = result.get();
          List<TaskResult> taskResults = revision.getResults();
          if (taskResults != null) {
            for (TaskResult resultItem : taskResults) {
              extractOutputProperty(task, results, resultItem);
            }
          }

        } else {
          Optional<Revision> latestRevision = revisions.stream()
              .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
          if (latestRevision.isPresent()) {
            List<TaskResult> taskResults = latestRevision.get().getResults();
            if (taskResults != null) {
              for (TaskResult resultItem : taskResults) {
                extractOutputProperty(task, results, resultItem);
              }
            }
          }
        }
      }
    } else {
      String activityId = task.getActivityId();
      ActivityEntity activity = workflowActivityService.findWorkflowActivtyById(activityId);
      String revisionId = activity.getWorkflowRevisionid();
      Optional<RevisionEntity> revisionEntity = this.revisionService.getRevision(revisionId);
      if (revisionEntity.isPresent()) {
        RevisionEntity revision = revisionEntity.get();

        List<DAGTask> tasks = revision.getDag().getTasks();
        DAGTask dagTask = tasks.stream().filter(x -> x.getTaskId().equals(task.getTaskId()))
            .findFirst().orElse(null);

        if (dagTask != null) {
          dagTask.getResults();
          List<TaskResult> dagResults = dagTask.getResults();
          if (dagResults != null) {
            for (TaskResult taskResult : dagResults) {

              TaskOutputResult outputResult = new TaskOutputResult();
              String key = taskResult.getName();

              outputResult.setName(key);
              outputResult.setDescription(taskResult.getDescription());

              if (task.getOutputs() != null && task.getOutputs().containsKey(key)) {
                outputResult.setValue(task.getOutputs().get(key));
              }

              results.add(outputResult);
            }
          }
        }
      }

    }


    response.setResults(results);
  }

  private void extractOutputProperty(TaskExecutionEntity task, List<TaskOutputResult> results,
      TaskResult resultItem) {
    String key = resultItem.getName();
    TaskOutputResult outputResult = new TaskOutputResult();
    outputResult.setName(key);
    outputResult.setDescription(resultItem.getDescription());
    if (task.getOutputs() != null && task.getOutputs().containsKey(key)) {
      outputResult.setValue(task.getOutputs().get(key));
    }
    results.add(outputResult);
  }

  @Override
  public TaskExecutionEntity saveTaskExecution(TaskExecutionEntity task) {
    return taskService.save(task);
  }

  @Override
  public InsightsSummary getInsightsSummary(Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<String> teamId) {

    final Page<ActivityEntity> records = flowActivityService.findAllActivities(from, to, pageable);
    final InsightsSummary response = new InsightsSummary();
    final List<FlowActivity> activities = convert(records.getContent());
    List<Execution> executions = new ArrayList<>();
    Long totalExecutionTime = 0L;
    Long executionTime;

    for (FlowActivity activity : activities) {

      executionTime = activity.getDuration();

      if (executionTime != null) {
        totalExecutionTime = totalExecutionTime + executionTime;
      }

      addActivityDetail(teamId, executions, activity);
    }
    response.setTotalActivitiesExecuted(executions.size());
    response.setExecutions(executions);

    if (response.getTotalActivitiesExecuted() != 0) {
      response.setMedianExecutionTime(totalExecutionTime / executions.size());

    } else {
      response.setMedianExecutionTime(0L);
    }
    return response;
  }

  private void addActivityDetail(Optional<String> teamId, List<Execution> executions,
      FlowActivity activity) {
    String teamName = null;
    String workflowName = null;
    String workflowId = activity.getWorkflowId();
    String activityTeamId = null;
    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
    if (workflow != null) {
      workflowName = workflow.getName();

      if (WorkflowScope.team.equals(workflow.getScope())) {
        activityTeamId = workflowService.getWorkflow(workflowId).getFlowTeamId();
        FlowTeamEntity team = flowTeamService.findById(activityTeamId);

        if (team != null) {
          teamName = team.getName();

        } else {
          teamName = null;
        }
      }
    }

    if (teamId.isPresent()) {
      String teamID = teamId.get();

      if (activityTeamId != null && teamID.equals(activityTeamId)) {
        Execution execution = createExecution(activity, teamName, workflowName, workflowId);
        executions.add(execution);
      }
    } else {
      Execution execution = createExecution(activity, teamName, workflowName, workflowId);
      executions.add(execution);
    }
  }

  private Execution createExecution(FlowActivity activity, String teamName, String workflowName,
      String workflowId) {
    Execution execution = new Execution();
    execution.setActivityId(activity.getId());
    execution.setStatus(activity.getStatus());
    execution.setDuration(activity.getDuration());
    execution.setCreationDate(activity.getCreationDate());
    execution.setTeamName(teamName);
    execution.setWorkflowName(workflowName);
    execution.setWorkflowId(workflowId);
    return execution;
  }

  @Override
  public StreamingResponseBody getTaskLog(String activityId, String taskId) {

    LOGGER.info("Getting task log for activity: {} task id: {}", activityId, taskId);

    TaskExecutionEntity taskExecution = taskService.findByTaskIdAndActivityId(taskId, activityId);

    ActivityEntity activity =
        workflowActivityService.findWorkflowActivtyById(taskExecution.getActivityId());

    List<String> removeList = buildRemovalList(taskId, taskExecution, activity);
    LOGGER.debug("Removal List Count: {} ", removeList.size());

    return outputStream -> {
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("workflowId", activity.getWorkflowId());
      requestParams.put("workflowActivityId", activityId);
      requestParams.put("taskActivityId", taskExecution.getId());
      requestParams.put("taskId", taskId);

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", controllerBaseUrl + getStreamDownloadPath + "?", ""));

      RequestCallback requestCallback = request -> request.getHeaders()
          .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

      PrintWriter printWriter = new PrintWriter(outputStream);

      ResponseExtractor<Void> responseExtractor =
          getResponseExtractorForRemovalList(removeList, outputStream, printWriter);
      LOGGER.info("Startingg log download: {}", encodedURL);
      try {
        restTemplate.execute(encodedURL, HttpMethod.GET, requestCallback, responseExtractor);
      } catch (Exception ex) {
        LOGGER.error("Error downloading logs: {} task id: {}", activityId, taskId);
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }

      LOGGER.info("Completed log download: {}", encodedURL);
    };
  }

  private ResponseExtractor<Void> getResponseExtractorForRemovalList(List<String> maskWordList,
      OutputStream outputStream, PrintWriter printWriter) {
    if (maskWordList.isEmpty()) {
      LOGGER.info("Remove word list empty, moving on.");
      return restTemplateResponse -> {
        InputStream is = restTemplateResponse.getBody();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
          outputStream.write(data, 0, nRead);
        }
        return null;
      };
    } else {
      LOGGER.info("Streaming response from controller and processing");
      return restTemplateResponse -> {
        try {
          InputStream is = restTemplateResponse.getBody();
          Reader reader = new InputStreamReader(is);
          BufferedReader bufferedReader = new BufferedReader(reader);
          String input = null;
          while ((input = bufferedReader.readLine()) != null) {

            printWriter.println(satanzieInput(input, maskWordList));
            if (!input.isBlank()) {
              printWriter.flush();
            }
          }
        } catch (Exception e) {
          LOGGER.error("Error streaming logs, displaying exception and moving on.");
          LOGGER.error(ExceptionUtils.getStackTrace(e));
        } finally {
          printWriter.close();
        }
        return null;
      };
    }
  }

  private List<String> buildRemovalList(String taskId, TaskExecutionEntity taskExecution,
      ActivityEntity activity) {

    String activityId = activity.getId();
    List<String> removalList = new LinkedList<>();
    Task task = new Task();
    task.setTaskId(taskId);
    task.setTaskType(taskExecution.getTaskType());

    ControllerRequestProperties applicationProperties =
        propertyManager.buildRequestPropertyLayering(task, activityId, activity.getWorkflowId());
    Map<String, String> map = applicationProperties.getMap(false);

    String workflowRevisionId = activity.getWorkflowRevisionid();

    Optional<RevisionEntity> revisionOptional = this.versionService.getRevision(workflowRevisionId);
    if (revisionOptional.isEmpty()) {
      return new LinkedList<>();
    }

    RevisionEntity revision = revisionOptional.get();
    Dag dag = revision.getDag();
    List<DAGTask> dagTasks = dag.getTasks();
    DAGTask dagTask =
        dagTasks.stream().filter((t) -> taskId.equals(t.getTaskId())).findFirst().orElse(null);
    if (dagTask != null) {
      if (dagTask.getTemplateId() != null) {
        FlowTaskTemplateEntity flowTaskTemplateEntity =
            templateService.getTaskTemplateWithId(dagTask.getTemplateId());
        if (flowTaskTemplateEntity != null && flowTaskTemplateEntity.getRevisions() != null) {
          Optional<Revision> latestRevision = flowTaskTemplateEntity.getRevisions().stream()
              .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
          if (latestRevision.isPresent()) {
            Revision rev = latestRevision.get();
            for (TaskTemplateConfig taskConfig : rev.getConfig()) {
              if ("password".equals(taskConfig.getType())) {
                LOGGER.debug("Found a secured property being used: {}", taskConfig.getKey());
                String key = taskConfig.getKey();
                String inputValue = map.get(key);
                if (inputValue == null || inputValue.isBlank()) {
                  inputValue = taskConfig.getDefaultValue();
                }
                String value = propertyManager.replaceValueWithProperty(inputValue, activityId,
                    applicationProperties);
                value = propertyManager.replaceValueWithProperty(value, activityId,
                    applicationProperties);
                LOGGER.debug("New Value: {}", value);
                if (!value.isBlank()) {
                  removalList.add(value);
                }
              }
            }
          }
        }
      }
    }

    LOGGER.debug("Displaying removal list");
    for (String item : removalList) {
      LOGGER.debug("Item: {}", item);
    }
    return removalList;
  }

  private String satanzieInput(String input, List<String> removeList) {
    for (String value : removeList) {
      input = input.replaceAll(Pattern.quote(value), "******");
    }
    return input;
  }

  @Override
  public void cancelWorkflowActivity(String activityId) {
    ActivityEntity activity = flowActivityService.findWorkflowActivtyById(activityId);
    activity.setStatus(TaskStatus.cancelled);

    flowActivityService.saveWorkflowActivity(activity);

    String workflowId = activity.getWorkflowId();
    final WorkflowEntity workflow = workflowService.getWorkflow(workflowId);

    List<TaskExecutionEntity> activites = taskService.findTaskActiivtyForActivity(activityId);
    for (TaskExecutionEntity taskExecution : activites) {
      if ((taskExecution.getTaskType() == TaskType.customtask
          || taskExecution.getTaskType() == TaskType.script
          || taskExecution.getTaskType() == TaskType.template)
          && taskExecution.getFlowTaskStatus() == TaskStatus.inProgress) {
        Task task = new Task();
        task.setTaskId(taskExecution.getTaskId());
        task.setTaskName(taskExecution.getTaskName());
        task.setWorkflowId(workflow.getId());
        task.setWorkflowName(workflow.getName());
        task.setTaskActivityId(taskExecution.getId());

        controllerClient.terminateTask(task);
      }

      if (taskExecution.getFlowTaskStatus() == TaskStatus.notstarted
          || taskExecution.getFlowTaskStatus() == TaskStatus.inProgress
          || taskExecution.getFlowTaskStatus() == TaskStatus.waiting) {
        taskExecution.setFlowTaskStatus(TaskStatus.cancelled);
      }
      taskService.save(taskExecution);
    }
  }

  @Override
  public List<FlowActivity> findActivty(Pageable pageable, Optional<String> labels) {

    List<Criteria> criteriaList = new ArrayList<>();

    if (labels.isPresent()) {
      String labelsValue = labels.get();
      String decodedLabels = "";
      try {
        decodedLabels = URLDecoder.decode(labelsValue, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }

      String[] splitString = decodedLabels.split("[,=]+");

      List<String> keys = new ArrayList<>();
      List<String> values = new ArrayList<>();

      for (String split : splitString) {
        if (Arrays.asList(splitString).indexOf(split) % 2 == 0) {
          keys.add(split);
        } else {
          values.add(split);
        }
      }

      for (String key : keys) {
        Criteria labelsKeyCriteria = Criteria.where("labels.key").is(key);
        criteriaList.add(labelsKeyCriteria);
      }
      for (String value : values) {
        Criteria labelsValueCriteria = Criteria.where("labels.value").is(value);
        criteriaList.add(labelsValueCriteria);
      }
    }
    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }

    Query activityQuery = new Query(allCriteria);
    activityQuery.with(pageable);

    Page<ActivityEntity> activityPages = PageableExecutionUtils.getPage(
        mongoTemplate.find(activityQuery.with(pageable), ActivityEntity.class), pageable,
        () -> mongoTemplate.count(activityQuery, ActivityEntity.class));

    List<FlowActivity> activityRecords = this.convert(activityPages.getContent());

    return activityRecords;
  }

}
