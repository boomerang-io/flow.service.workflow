package io.boomerang.service.crud;

import java.util.List;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.DuplicateRequest;
import io.boomerang.model.FlowWorkflowRevision;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.TemplateWorkflowSummary;
import io.boomerang.model.UserWorkflowSummary;
import io.boomerang.model.WorkflowExport;
import io.boomerang.model.WorkflowShortSummary;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.mongo.model.WorkflowScope;

public interface WorkflowService {

  void deleteWorkflow(String workFlowid);

  WorkflowSummary getWorkflow(String workflowId);

  List<WorkflowSummary> getWorkflowsForTeam(String flowTeamId);

  WorkflowSummary saveWorkflow(WorkflowEntity flowWorkflowEntity);

  WorkflowSummary updateWorkflow(WorkflowSummary summary);

  WorkflowSummary updateWorkflowProperties(String workflowId, List<WorkflowProperty> properties);

  GenerateTokenResponse generateTriggerToken(String id, String label);

  ResponseEntity<InputStreamResource> exportWorkflow(String workFlowId);

  void importWorkflow(WorkflowExport export, Boolean update, String flowTeamId, WorkflowScope scope);
  
  boolean canExecuteWorkflowForQuotas(String teamId);

  boolean canExecuteWorkflow(String workFlowId, Optional<String> trigger);
  
  public List<WorkflowShortSummary> getWorkflowShortSummaryList();

  ResponseEntity<HttpStatus> validateWorkflowToken(String id, GenerateTokenResponse tokenPayload);

  void deleteToken(String id, String label);

  List<WorkflowSummary> getSystemWorkflows();
  
  UserWorkflowSummary getUserWorkflows();

  List<WorkflowShortSummary> getSystemWorkflowShortSummaryList();

  List<String> getWorkflowParameters(String workFlowId);

  List<String> getWorkflowParameters(String workflowId, FlowWorkflowRevision workflowSummaryEntity);

  WorkflowSummary duplicateWorkflow(String id, DuplicateRequest duplicateRequest);

  boolean canExecuteWorkflowForQuotasForUser(String workflowId);

  List<TemplateWorkflowSummary> getTemplateWorkflows();

  CronValidationResponse validateCron(String cron);

  UserWorkflowSummary getUserWorkflows(String userId);
}
