package net.boomerangplatform.mongo.service;

import java.util.List;
import net.boomerangplatform.mongo.entity.WorkflowEntity;

public interface FlowWorkflowService {

  void deleteWorkflow(String id);

  WorkflowEntity getWorkflow(String id);

  List<WorkflowEntity> getAllWorkflows();
  
  List<WorkflowEntity> getWorkflowsForTeams(String flowId);

  List<WorkflowEntity> getWorkflowsForTeams(List<String> flowTeamIds);

  List<WorkflowEntity> getScheduledWorkflows();

  List<WorkflowEntity> getEventWorkflows();

  List<WorkflowEntity> getEventWorkflowsForTopic(String topic);

  WorkflowEntity saveWorkflow(WorkflowEntity entity);

  WorkflowEntity findByTokenString(String tokenString);


}
