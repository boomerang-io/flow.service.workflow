package net.boomerangplatform.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.Approval;
import net.boomerangplatform.model.ApprovalRequest;
import net.boomerangplatform.model.ApprovalStatus;
import net.boomerangplatform.model.FlowTeam;
import net.boomerangplatform.model.TeamWorkflowSummary;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.ApprovalEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.model.Audit;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.ApprovalService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.crud.TeamService;
import net.boomerangplatform.service.crud.WorkflowService;
import net.boomerangplatform.service.refactor.ControllerRequestProperties;
import net.boomerangplatform.service.refactor.TaskClient;
import net.boomerangplatform.model.Task;

@Service
public class FlowApprovalServiceImpl implements FlowApprovalService {

  @Autowired
  private ApprovalService approvalService;

  @Autowired
  private TaskClient taskClient;

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

      InternalTaskResponse actionApprovalResponse = new InternalTaskResponse();
      actionApprovalResponse.setActivityId(approvalEntity.getTaskActivityId());
      if (request.isApproved()) {
        approvalEntity.setStatus(ApprovalStatus.approved);
        actionApprovalResponse.setStatus(TaskStatus.completed);
      } else {
        approvalEntity.setStatus(ApprovalStatus.rejected);
        actionApprovalResponse.setStatus(TaskStatus.failure);
      }
      approvalService.save(approvalEntity);
      taskClient.endTask(actionApprovalResponse);
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
      CoreProperty instructionsProperty = dagTask.getProperties().stream()
          .filter(p -> "instructions".equals(p.getKey())).findFirst().orElse(null);
      if (instructionsProperty != null) {
        String instructionText = instructionsProperty.getValue();
        
        if (instructionText != null) {
          String activityId = activity.getId();
          Task task = new Task();
          task.setTaskId(taskExecution.getTaskId());
          task.setTaskType(taskExecution.getTaskType());
          task.setTaskName(taskExecution.getTaskName());
          ControllerRequestProperties properties = propertyManager.buildRequestPropertyLayering(null, activityId, activity.getWorkflowId());
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
