package io.boomerang.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.Approval;
import io.boomerang.model.ApprovalRequest;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.model.FlowTeam;
import io.boomerang.model.Task;
import io.boomerang.model.TeamWorkflowSummary;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.ApprovalEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.Audit;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.internal.InternalTaskResponse;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.ApprovalService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.TeamService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.ControllerRequestProperties;
import io.boomerang.service.refactor.TaskClient;
import io.boomerang.service.refactor.TaskService;

@Service
public class FlowApprovalServiceImpl implements FlowApprovalService {

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
      
      SimpleDateFormat formatter =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZZZZ");
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

  @Override
  public List<Approval> getApprovalsForUser() {
    FlowUserEntity flowUser = userIdentityService.getCurrentUser();
    List<Approval> approvals = new LinkedList<>();
    List<TeamWorkflowSummary> teams = teamService.getUserTeams(flowUser);
    for (TeamWorkflowSummary team : teams) {
      String flowId = team.getId();
      List<Approval> teamApprovals = this.getApprovalsForTeam(flowId);
      approvals.addAll(teamApprovals);
    }
    return approvals;
  }

  @Override
  public List<Approval> getApprovalsForTeam(String teamId) {
    List<ApprovalEntity> approvalsList = approvalService.getActiivtyForTeam(teamId);
    List<Approval> approvals = new LinkedList<>();
    for (ApprovalEntity approvalEntity : approvalsList) {
      Approval approval = convertToApproval(approvalEntity);
      approvals.add(approval);
    }
    return approvals;
  }

  private Approval convertToApproval(ApprovalEntity approvalEntity) {
    Approval approval = new Approval();
    approval.setId(approvalEntity.getId());
    approval.setAudit(approvalEntity.getAudit());
    approval.setActivityId(approvalEntity.getActivityId());
    approval.setTaskActivityId(approvalEntity.getTaskActivityId());
    approval.setWorkflowId(approvalEntity.getWorkflowId());
    approval.setTeamId(approvalEntity.getTeamId());
    approval.setStatus(approvalEntity.getStatus());

    if (approval.getAudit() != null) {
      Audit audit = approval.getAudit();
      FlowUserEntity flowUser = userIdentityService.getUserByID(audit.getApproverId());
      audit.setApproverEmail(flowUser.getEmail());
      audit.setApproverName(flowUser.getName());
    }
    WorkflowSummary workflowSummary = workflowService.getWorkflow(approval.getWorkflowId());
    approval.setWorkflowName(workflowSummary.getName());

    FlowTeam flowTeam = teamService.getTeamById(approval.getTeamId());
    approval.setTeamName(flowTeam.getName());
    approval.setTaskName("");

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
          ControllerRequestProperties properties = propertyManager.buildRequestPropertyLayering(task, activityId, activity.getWorkflowId());
          instructionText = propertyManager.replaceValueWithProperty(instructionText, activityId, properties);
        }
        approval.setInstructions(instructionText);
      }
    }
    
    return approval;
  }

  @Override
  public Approval getApprovalById(String id) {
    ApprovalEntity approvalEntity = this.approvalService.findById(id);
    return this.convertToApproval(approvalEntity);
  }

  @Override
  public Approval getApprovalByTaskActivityId(String id) {
    ApprovalEntity approvalEntity = this.approvalService.findByTaskActivityId(id);
    if (approvalEntity == null) {
      return null;
    }
    return convertToApproval(approvalEntity);
  }
}
