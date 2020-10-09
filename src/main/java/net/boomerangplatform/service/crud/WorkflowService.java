package net.boomerangplatform.service.crud;

import java.util.List;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import net.boomerangplatform.model.GenerateTokenResponse;
import net.boomerangplatform.model.WorkflowExport;
import net.boomerangplatform.model.WorkflowShortSummary;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;

public interface WorkflowService {

  void deleteWorkflow(String workFlowid);

  WorkflowSummary getWorkflow(String workflowId);

  List<WorkflowSummary> getWorkflowsForTeam(String flowTeamId);

  WorkflowSummary saveWorkflow(WorkflowEntity flowWorkflowEntity);

  WorkflowSummary updateWorkflow(WorkflowSummary summary);

  WorkflowSummary updateWorkflowProperties(String workflowId, List<FlowProperty> properties);

  GenerateTokenResponse generateTriggerToken(String id, String label);

  ResponseEntity<InputStreamResource> exportWorkflow(String workFlowId);

  void importWorkflow(WorkflowExport export, Boolean update, String flowTeamId);
  
  boolean canExecuteWorkflowForQuotas(String teamId);

  boolean canExecuteWorkflow(String workFlowId, Optional<String> trigger);
  
  public List<WorkflowShortSummary> getWorkflowShortSummaryList();

  ResponseEntity<HttpStatus> validateWorkflowToken(String id, GenerateTokenResponse tokenPayload);

  void deleteToken(String id, String label);
}
