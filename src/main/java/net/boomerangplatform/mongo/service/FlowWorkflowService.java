package net.boomerangplatform.mongo.service;

import java.util.List;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;

public interface FlowWorkflowService {

  void deleteWorkflow(String id);

  FlowWorkflowEntity getWorkflow(String id);

  List<FlowWorkflowEntity> getWorkflowsForTeams(String flowId);

  List<FlowWorkflowEntity> getWorkflowsForTeams(List<String> flowTeamIds);

  List<FlowWorkflowEntity> getScheduledWorkflows();

  List<FlowWorkflowEntity> getEventWorkflows();

  List<FlowWorkflowEntity> getEventWorkflowsForTopic(String topic);

  FlowWorkflowEntity saveWorkflow(FlowWorkflowEntity entity);

  FlowWorkflowEntity findByTokenString(String tokenString);


}
