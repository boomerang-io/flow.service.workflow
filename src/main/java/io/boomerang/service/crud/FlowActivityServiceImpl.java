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
import java.util.concurrent.TimeUnit;
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
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.ListActivityResponse;
import io.boomerang.model.Sort;
import io.boomerang.model.Task;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.TaskOutputResult;
import io.boomerang.model.controller.TaskResult;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.model.teams.Action;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.FlowTriggerEnum;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskTemplateConfig;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.ActionService;
import io.boomerang.service.EventingService;
import io.boomerang.service.FilterService;
import io.boomerang.service.PropertyManager;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.refactor.ControllerRequestProperties;
import io.boomerang.service.runner.misc.ControllerClient;
import io.boomerang.util.DateUtil;
import io.boomerang.util.ParameterMapper;

@Service
public class FlowActivityServiceImpl implements FlowActivityService {

  @Value("${controller.rest.url.base}")
  private String controllerBaseUrl;

  @Value("${controller.rest.url.streamlogs}")
  private String getStreamDownloadPath;

  @Value("${max.workflow.duration}")
  private long maxWorkflowDuration;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  @Autowired
  private FlowSettingsService flowSettingsService;

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
  private FilterService filterService;

  @Autowired
  private ActionService approvalService;

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

  @Autowired
  private EventingService eventingService;

  private static final Logger LOGGER = LogManager.getLogger();

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
    activity.setStatus(TaskStatus.notstarted);

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
      List<KeyValuePair> propertyList =
          ParameterMapper.mapToKeyValuePairList(request.getProperties());
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
    List<String> workflowIdsList =
        filterService.getFilteredWorkflowIds(workflowIds, teamIds, scopes);


    ListActivityResponse response = new ListActivityResponse();
    Page<ActivityEntity> records = flowActivityService.getAllActivities(from, to, page,
        Optional.of(workflowIdsList), statuses, triggers);
    final List<FlowActivity> activities =
        filterService.convertActivityEntityToFlowActivity(records.getContent());
    List<FlowActivity> activitiesFiltered = new ArrayList<>();
    for (FlowActivity activity : activities) {
      String workFlowId = activity.getWorkflowId();
      addTeamInformation(teamIds, activitiesFiltered, activity, workFlowId);
    }

    io.boomerang.model.Pageable pageablefinal =
        createPageable(records, property, direction, activitiesFiltered, activitiesFiltered.size());
    response.setPageable(pageablefinal);
    response.setRecords(activities);

    return response;
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
  public Map<String, Long> getActivitySummary(Pageable pageable, Optional<List<String>> teamIds,
      List<String> triggers, Optional<List<String>> workflowIds, Optional<List<String>> scopes,
      Long fromDate, Long toDate) {

    List<String> workflowIdsList =
        filterService.getFilteredWorkflowIds(workflowIds, teamIds, scopes);
    Optional<Date> to =
        toDate == null ? Optional.empty() : Optional.of(DateUtil.asDate(getDateTime(toDate)));
    Optional<Date> from =
        fromDate == null ? Optional.empty() : Optional.of(DateUtil.asDate(getDateTime(fromDate)));


    List<ActivityEntity> flowWorkflowActivityEntities =
        flowActivityService.getAllActivities(from, to, pageable, getOptional(workflowIdsList),
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

      TeamEntity team = teamService.getTeamById(teamId);
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

    final List<FlowActivity> activities =
        filterService.convertActivityEntityToFlowActivity(records.getContent());
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
        Action approval = approvalService.getApprovalByTaskActivityId(task.getId());
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
        if (task.getOutputs() != null) {
          String json = task.getOutputs().get("eventPayload");
          result.setValue(json);
        }
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
  public void cancelWorkflowActivity(String activityId, ErrorResponse error) {
    ActivityEntity activity = flowActivityService.findWorkflowActivtyById(activityId);
    activity.setStatus(TaskStatus.cancelled);
    Optional.ofNullable(error).ifPresent(activity::setError);

    flowActivityService.saveWorkflowActivity(activity);
    eventingService.publishWorkflowActivityStatusUpdateCE(activity);

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

    List<FlowActivity> activityRecords =
        filterService.convertActivityEntityToFlowActivity(activityPages.getContent());

    return activityRecords;
  }

  @Override
  public boolean hasExceededExecutionQuotas(String activityId) {

    ActivityEntity activity = flowActivityService.findWorkflowActivtyById(activityId);
    String workflowId = activity.getWorkflowId();
    final WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
    WorkflowScope scope = workflow.getScope();
    if (scope == WorkflowScope.system) {
      return false;
    }

    long maxDuration = TimeUnit.MINUTES.toMillis(this.maxWorkflowDuration);
    if (scope == WorkflowScope.user) {
      maxDuration = TimeUnit.MINUTES.toMillis(Integer.parseInt(
          flowSettingsService.getConfiguration("users", "max.user.workflow.duration").getValue()));

    } else if (scope == WorkflowScope.team) {
      maxDuration = TimeUnit.MINUTES.toMillis(teamService.getTeamById(workflow.getFlowTeamId())
          .getQuotas().getMaxWorkflowExecutionTime());
    }

    List<TaskExecutionEntity> activites = taskService.findTaskActiivtyForActivity(activityId);

    long totalDuration = 0;

    for (TaskExecutionEntity task : activites) {
      if (task.getTaskType() == TaskType.template || task.getTaskType() == TaskType.customtask) {

        if (task.getFlowTaskStatus() == TaskStatus.completed
            || task.getFlowTaskStatus() == TaskStatus.failure) {
          totalDuration += task.getDuration();
        } else if (task.getFlowTaskStatus() == TaskStatus.inProgress) {
          Date currentTime = new Date();
          long inProgressTime = currentTime.getTime() - task.getStartTime().getTime();
          totalDuration += inProgressTime;
        }
      }
    }

    if (maxDuration < totalDuration) {
      return true;
    }
    return false;
  }

}
