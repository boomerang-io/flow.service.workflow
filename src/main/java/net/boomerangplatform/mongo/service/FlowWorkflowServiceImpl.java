package net.boomerangplatform.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.repository.FlowWorkflowRepository;

@Service
public class FlowWorkflowServiceImpl implements FlowWorkflowService {

  @Autowired
  private FlowWorkflowRepository workFlowRepository;

  @Override
  public void deleteWorkflow(String id) {
    workFlowRepository.deleteById(id);
  }

  @Override
  public FlowWorkflowEntity getWorkflow(String id) {
    return workFlowRepository.findById(id).orElse(null);
  }

  @Override
  public List<FlowWorkflowEntity> getWorkflowsForTeams(String flowId) {
    return workFlowRepository.findByFlowTeamId(flowId);
  }

  @Override
  public List<FlowWorkflowEntity> getWorkflowsForTeams(List<String> flowTeamIds) {
    return workFlowRepository.findByFlowTeamIdIn(flowTeamIds);
  }

  @Override
  public FlowWorkflowEntity saveWorkflow(FlowWorkflowEntity entity) {
    return workFlowRepository.save(entity);
  }

  @Override
  public FlowWorkflowEntity findByTokenString(String tokenString) {
    return workFlowRepository.findByToken(tokenString);
  }

  @Override
  public List<FlowWorkflowEntity> getScheduledWorkflows() {
    return workFlowRepository.findAllScheduledWorkflows();
  }

  @Override
  public List<FlowWorkflowEntity> getEventWorkflows() {
    return workFlowRepository.findAllEventWorkflows();
  }

  @Override
  public List<FlowWorkflowEntity> getEventWorkflowsForTopic(String topic) {
    return workFlowRepository.findAllEventWorkflowsForTopic(topic);
  }


}
