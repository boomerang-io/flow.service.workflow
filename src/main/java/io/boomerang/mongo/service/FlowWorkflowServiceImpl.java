package io.boomerang.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.repository.FlowWorkflowRepository;

@Service
public class FlowWorkflowServiceImpl implements FlowWorkflowService {

  @Autowired
  private FlowWorkflowRepository workFlowRepository;

  @Override
  public void deleteWorkflow(String id) {
    workFlowRepository.deleteById(id);
  }

  @Override
  public WorkflowEntity getWorkflow(String id) {
    return workFlowRepository.findById(id).orElse(null);
  }

  @Override
  public List<WorkflowEntity> getWorkflowsForTeams(String flowId) {
    return workFlowRepository.findByFlowTeamId(flowId);
  }

  @Override
  public List<WorkflowEntity> getWorkflowsForTeams(List<String> flowTeamIds) {
    return workFlowRepository.findByFlowTeamIdIn(flowTeamIds);
  }

  @Override
  public WorkflowEntity saveWorkflow(WorkflowEntity entity) {
    return workFlowRepository.save(entity);
  }

  @Override
  public WorkflowEntity findByTokenString(String tokenString) {
    return workFlowRepository.findByToken(tokenString);
  }

  @Override
  public List<WorkflowEntity> getScheduledWorkflows() {
    return workFlowRepository.findAllScheduledWorkflows();
  }

  @Override
  public List<WorkflowEntity> getEventWorkflows() {
    return workFlowRepository.findAllEventWorkflows();
  }

  @Override
  public List<WorkflowEntity> getEventWorkflowsForTopic(String topic) {
    return workFlowRepository.findAllEventWorkflowsForTopic(topic);
  }

  @Override
  public List<WorkflowEntity> getAllWorkflows() {
    return workFlowRepository.findAll();
  }

  @Override
  public List<WorkflowEntity> getSystemWorkflows() {
    return workFlowRepository.findByScope(WorkflowScope.system);
  }

  @Override
  public List<WorkflowEntity> getUserWorkflows(String id) {
    return workFlowRepository.findByScopeAndOwnerUserId(WorkflowScope.user, id);
  }
}
