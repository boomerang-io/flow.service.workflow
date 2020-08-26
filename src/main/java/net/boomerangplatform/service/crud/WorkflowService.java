package net.boomerangplatform.service.crud;

import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import net.boomerangplatform.model.GenerateTokenResponse;
import net.boomerangplatform.model.WorkflowExport;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.model.FlowProperty;

public interface WorkflowService {

  void deleteWorkflow(String workFlowid);

  WorkflowSummary getWorkflow(String workflowId);

  List<WorkflowSummary> getWorkflowsForTeam(String flowTeamId);

  WorkflowSummary saveWorkflow(FlowWorkflowEntity flowWorkflowEntity);

  WorkflowSummary updateWorkflow(WorkflowSummary summary);

  WorkflowSummary updateWorkflowProperties(String workflowId, List<FlowProperty> properties);

  GenerateTokenResponse generateWebhookToken(String id);

  ResponseEntity<InputStreamResource> exportWorkflow(String workFlowId);

  void importWorkflow(WorkflowExport export, Boolean update, String flowTeamId);

  boolean canExecuteWorkflow(String teamId);

}
