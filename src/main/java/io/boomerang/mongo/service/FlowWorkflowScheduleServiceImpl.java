package io.boomerang.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.mongo.repository.FlowWorkflowScheduleRepository;

@Service
public class FlowWorkflowScheduleServiceImpl implements FlowWorkflowScheduleService {

  @Autowired
  private FlowWorkflowScheduleRepository workflowScheduleRepository;

  @Override
  public void deleteSchedule(String id) {
    workflowScheduleRepository.deleteById(id);
  }

  @Override
  public WorkflowScheduleEntity getSchedule(String id) {
    return workflowScheduleRepository.findById(id).orElse(null);
  }

  @Override
  public List<WorkflowScheduleEntity> getSchedulesForWorkflow(String workflowId) {
    return workflowScheduleRepository.findByWorkflowId(workflowId);
  }

  @Override
  public WorkflowScheduleEntity saveSchedule(WorkflowScheduleEntity entity) {
    return workflowScheduleRepository.save(entity);
  }
}
