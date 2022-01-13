package io.boomerang.mongo.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.mongo.model.WorkflowScheduleStatus;
import io.boomerang.mongo.repository.FlowWorkflowScheduleRepository;

@Service
public class ScheduleServiceImpl implements ScheduleService {

  @Autowired
  private FlowWorkflowScheduleRepository workflowScheduleRepository;
  
  @Autowired
  private MongoTemplate mongoTemplate;
  
  private List<WorkflowScheduleStatus> getStatusesNotCompletedOrDeleted() {
    List<WorkflowScheduleStatus> statuses = new LinkedList<>();
    statuses.add(WorkflowScheduleStatus.active);
    statuses.add(WorkflowScheduleStatus.inactive);
    statuses.add(WorkflowScheduleStatus.trigger_disabled);
    return statuses;
  }

  @Override
  public void deleteSchedule(String id) {
    workflowScheduleRepository.deleteById(id);
  }

  @Override
  public WorkflowScheduleEntity getSchedule(String id) {
    return workflowScheduleRepository.findById(id).orElse(null);
  }

  @Override
  public List<WorkflowScheduleEntity> getSchedules(List<String> ids) {
    return workflowScheduleRepository.findByIdIn(ids);
  }
  
  @Override
  public List<WorkflowScheduleEntity> getSchedulesNotCompletedOrDeleted(List<String> ids) {
    return workflowScheduleRepository.findByIdInAndStatusIn(ids, getStatusesNotCompletedOrDeleted());
  }

  @Override
  public List<WorkflowScheduleEntity> getSchedulesForWorkflow(String workflowId) {
    return workflowScheduleRepository.findByWorkflowId(workflowId);
  }
  
  @Override
  public List<WorkflowScheduleEntity> getSchedulesForWorkflowNotCompletedOrDeleted(String workflowId) {
    return workflowScheduleRepository.findByWorkflowIdAndStatusIn(workflowId, getStatusesNotCompletedOrDeleted());
  }

  @Override
  public WorkflowScheduleEntity saveSchedule(WorkflowScheduleEntity entity) {
    return workflowScheduleRepository.save(entity);
  }

  @Override
  public List<WorkflowScheduleEntity> getSchedulesForWorkflowWithStatus(String workflowId, WorkflowScheduleStatus status) {
    return workflowScheduleRepository.findByWorkflowIdAndStatus(workflowId, status);
  }

  @Override
  public List<WorkflowScheduleEntity> getAllSchedulesNotCompletedOrDeleted(List<String> workflowIds,
      Optional<List<String>> statuses, Optional<List<String>> types) {
    Criteria criteria = this.buildAllSchedulesCriteriaList(workflowIds, statuses, types);
    Query query = new Query(criteria);
    List<WorkflowScheduleEntity> list = this.mongoTemplate.find(query, WorkflowScheduleEntity.class);
    return list;
  }

  private Criteria buildAllSchedulesCriteriaList(List<String> workflowIds,
      Optional<List<String>> statuses, Optional<List<String>> types) {
    List<Criteria> criterias = new ArrayList<>();
    
    if (types.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("type").in(types.get());
      criterias.add(dynamicCriteria);
    }
    
    if (statuses.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("status")
          .in(statuses.get()
          .removeIf(s -> s.equals(WorkflowScheduleStatus.deleted.toString())
              || s.equals(WorkflowScheduleStatus.completed.toString())));
      criterias.add(dynamicCriteria);
    }
    
    Criteria workflowIdsCriteria = Criteria.where("workflowId").in(workflowIds);
    criterias.add(workflowIdsCriteria);
    return new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
  }
}
