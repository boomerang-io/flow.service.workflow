package io.boomerang.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import io.boomerang.model.ApprovalRequest;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.model.FlowTeam;
import io.boomerang.model.ListActionResponse;
import io.boomerang.model.Sort;
import io.boomerang.model.Task;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.teams.Action;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.ApprovalEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.Audit;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.ManualType;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.internal.InternalTaskResponse;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.ApprovalService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.TeamService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.ControllerRequestProperties;
import io.boomerang.service.refactor.TaskClient;
import io.boomerang.service.refactor.TaskService;

@Service
public class ActionServiceImpl implements ActionService {

  @Autowired
  private ApprovalService approvalService;

  @Autowired
  private TaskClient taskClient;

  @Autowired
  private TaskService taskService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private TeamService teamService;

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private ActivityTaskService activityTaskService;

  @Autowired
  private FlowActivityService activityService;

  @Autowired
  private RevisionService revisionService;

  @Autowired
  private PropertyManager propertyManager;

  @Autowired
  private FlowWorkflowService flowWorkflowService;

  @Override
  public void actionApproval(ApprovalRequest request) {

    FlowUserEntity flowUser = userIdentityService.getCurrentUser();
    ApprovalEntity approvalEntity = approvalService.findById(request.getId());
    if (approvalEntity != null && flowUser != null) {

      Audit audit = new Audit();
      audit.setActionDate(new Date());
      audit.setApproverId(flowUser.getId());
      audit.setComments(request.getComments());

      approvalEntity.setAudit(audit);
      Map<String, String> outputProperties = new HashMap<>();

      outputProperties.put("approvalUserName", flowUser.getName());
      outputProperties.put("approvalUserEmail", flowUser.getEmail());


      InternalTaskResponse actionApprovalResponse = new InternalTaskResponse();
      actionApprovalResponse.setActivityId(approvalEntity.getTaskActivityId());

      outputProperties.put("approvalComments", audit.getComments());

      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZZZZ");
      String strDate = formatter.format(audit.getActionDate());

      outputProperties.put("approvalDate", strDate);

      if (request.isApproved()) {
        outputProperties.put("approvalStatus", ApprovalStatus.approved.toString());
        approvalEntity.setStatus(ApprovalStatus.approved);
        actionApprovalResponse.setStatus(TaskStatus.completed);
      } else {
        outputProperties.put("approvalStatus", ApprovalStatus.rejected.toString());
        approvalEntity.setStatus(ApprovalStatus.rejected);
        actionApprovalResponse.setStatus(TaskStatus.failure);
      }
      actionApprovalResponse.setOutputProperties(outputProperties);
      approvalService.save(approvalEntity);
      taskClient.endTask(taskService, actionApprovalResponse);
    }
  }

  private Action convertToApproval(ApprovalEntity approvalEntity) {

    Action approval = new Action();
    approval.setId(approvalEntity.getId());
    approval.setAudit(approvalEntity.getAudit());
    approval.setActivityId(approvalEntity.getActivityId());
    approval.setTaskActivityId(approvalEntity.getTaskActivityId());
    approval.setWorkflowId(approvalEntity.getWorkflowId());
    approval.setTeamId(approvalEntity.getTeamId());
    approval.setStatus(approvalEntity.getStatus());
    approval.setType(approvalEntity.getType());
    
    if (approval.getAudit() != null) {
      Audit audit = approval.getAudit();
      FlowUserEntity flowUser = userIdentityService.getUserByID(audit.getApproverId());
      audit.setApproverEmail(flowUser.getEmail());
      audit.setApproverName(flowUser.getName());
    }
    WorkflowSummary workflowSummary = workflowService.getWorkflow(approval.getWorkflowId());
    approval.setWorkflowName(workflowSummary.getName());

    if (approval.getTeamId() != null) {
      FlowTeam flowTeam = teamService.getTeamById(approval.getTeamId());
      approval.setTeamName(flowTeam.getName());
      approval.setTaskName("");
    }
    
    TaskExecutionEntity taskExecution = activityTaskService.findById(approval.getTaskActivityId());
    approval.setTaskName(taskExecution.getTaskName());

    ActivityEntity activity =
        this.activityService.findWorkflowActivity(taskExecution.getActivityId());

    String revisionId = activity.getWorkflowRevisionid();

    RevisionEntity revision = this.revisionService.getWorkflowlWithId(revisionId);

    List<DAGTask> tasks = revision.getDag().getTasks();
    DAGTask dagTask = tasks.stream().filter(t -> t.getTaskId().equals(taskExecution.getTaskId()))
        .findFirst().orElse(null);
    if (dagTask != null && dagTask.getProperties() != null) {
      KeyValuePair instructionsProperty = dagTask.getProperties().stream()
          .filter(p -> "instructions".equals(p.getKey())).findFirst().orElse(null);
      if (instructionsProperty != null) {
        String instructionText = instructionsProperty.getValue();

        if (instructionText != null) {
          String activityId = activity.getId();
          Task task = new Task();
          task.setTaskId(taskExecution.getTaskId());
          task.setTaskType(taskExecution.getTaskType());
          task.setTaskName(taskExecution.getTaskName());
          ControllerRequestProperties properties = propertyManager
              .buildRequestPropertyLayering(task, activityId, activity.getWorkflowId());
          instructionText =
              propertyManager.replaceValueWithProperty(instructionText, activityId, properties);
        }
        approval.setInstructions(instructionText);
      }
    }

    return approval;
  }

  @Override
  public Action getApprovalById(String id) {
    ApprovalEntity approvalEntity = this.approvalService.findById(id);
    return this.convertToApproval(approvalEntity);
  }

  @Override
  public Action getApprovalByTaskActivityId(String id) {
    ApprovalEntity approvalEntity = this.approvalService.findByTaskActivityId(id);
    if (approvalEntity == null) {
      return null;
    }
    return convertToApproval(approvalEntity);
  }

  @Override
  public ListActionResponse getAllActions(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<String>> workflowIds, Optional<List<String>> teamIds, Optional<ManualType> type,
      Optional<List<String>> scopes, String property, Direction direction, Optional<ApprovalStatus> status) {

    List<String> workflowIdsList = getWorkflowIdsForParams(workflowIds, teamIds, scopes);
    ListActionResponse response = new ListActionResponse();
   
    Page<ApprovalEntity> records = this.approvalService.getAllApprovals(from, to, pageable, workflowIdsList, type, status);

    List<Action> actions = new LinkedList<>();
    
    for (ApprovalEntity approval : records.getContent()) {
      Action action = this.convertToApproval(approval);
      actions.add(action);
    }
    
    io.boomerang.model.Pageable pageablefinal =
        createPageable(records, property, direction, actions, actions.size());
    response.setPageable(pageablefinal);
    response.setRecords(actions);
    return response;
  }
  
  protected io.boomerang.model.Pageable createPageable(final Page<ApprovalEntity> records,
      String property, Direction direction, List<Action> actions, int totalElements) {
    
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

  private List<String> getWorkflowIdsForParams(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes) {
    final FlowUserEntity user = userIdentityService.getCurrentUser();
    List<String> workflowIdsList = new LinkedList<>();

    if (!workflowIds.isPresent()) {
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

        addUserWorkflows(user, workflowIdsList);
        addTeamWorkflows(user, workflowIdsList, teamIds);
        if (user.getType() == UserType.admin) {
          addSystemWorkflows(workflowIdsList);
        }
      }
    } else {
      List<String> requestWorkflowList = workflowIds.get();
      workflowIdsList.addAll(requestWorkflowList);
    }
    return workflowIdsList;
  }

  private void addTeamWorkflows(final FlowUserEntity user, List<String> workflowIdsList,
      Optional<List<String>> teamIds) {

    if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
      List<WorkflowEntity> allTeamWorkflows =
          this.flowWorkflowService.getWorkflowsForTeams(teamIds.get());
      List<String> allTeamWorkflowsIds =
          allTeamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
      workflowIdsList.addAll(allTeamWorkflowsIds);
    } else {
      if (user.getType() == UserType.admin) {
        List<WorkflowEntity> allTeamWorkflows = this.flowWorkflowService.getTeamWorkflows();
        List<String> workflowIds =
            allTeamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
        workflowIdsList.addAll(workflowIds);
      } else {
        List<TeamEntity> flowTeam = teamService.getUsersTeamListing(user);
        List<String> flowTeamIds =
            flowTeam.stream().map(TeamEntity::getId).collect(Collectors.toList());
        List<WorkflowEntity> teamWorkflows =
            this.flowWorkflowService.getWorkflowsForTeams(flowTeamIds);
        List<String> allTeamWorkflowsIds =
            teamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
        workflowIdsList.addAll(allTeamWorkflowsIds);
      }
    }
  }

  private void addSystemWorkflows(List<String> workflowIdsList) {
    List<WorkflowEntity> systemWorkflows = this.flowWorkflowService.getSystemWorkflows();
    List<String> systemWorkflowsIds =
        systemWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(systemWorkflowsIds);
  }

  private void addUserWorkflows(final FlowUserEntity user, List<String> workflowIdsList) {
    String userId = user.getId();
    List<WorkflowEntity> userWorkflows = this.flowWorkflowService.getUserWorkflows(userId);
    List<String> userWorkflowIds =
        userWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(userWorkflowIds);
  }
}
