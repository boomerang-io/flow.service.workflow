package net.boomerangplatform.mongo.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.model.TaskStatus;

public interface FlowWorkflowActivityRepository
    extends MongoRepository<ActivityEntity, String> {

  @Query(value = "{'creationDate':{ $lt: ?1, $gte: ?0}}")
  Page<ActivityEntity> findAll(Date fromDate, Date toDate, Pageable pageable);

  Page<ActivityEntity> findByworkflowId(String workflowId, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusInAndTriggerIn(
      List<String> workflowIds, List<TaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Pageable page);

  Page<ActivityEntity> findByTriggerIn(List<FlowTriggerEnum> triggers, Pageable page);

  Page<ActivityEntity> findByStatusIn(List<TaskStatus> statuses, Pageable page);

  Page<ActivityEntity> findByStatusInAndTriggerIn(List<TaskStatus> statuses,
      List<FlowTriggerEnum> triggers, Pageable page);

  Page<ActivityEntity> findByWorkflowIdIn(List<String> workflowIds, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndTriggerIn(List<String> workflowIds,
      List<FlowTriggerEnum> triggers, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusIn(List<String> workflowIds,
      List<TaskStatus> statuses, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateAfter(
      List<String> workflowIds, List<TaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Date from, Pageable page);

  Page<ActivityEntity> findByTriggerInAndCreationDateAfter(
      List<FlowTriggerEnum> triggers, Date from, Pageable page);

  Page<ActivityEntity> findByStatusInAndCreationDateAfter(List<TaskStatus> statuses,
      Date date, Pageable page);

  Page<ActivityEntity> findByStatusInAndTriggerInAndCreationDateAfter(
      List<TaskStatus> statuses, List<FlowTriggerEnum> triggers, Date date, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndCreationDateAfter(List<String> workflowIds,
      Date from, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndTriggerInAndCreationDateAfter(
      List<String> workflowIds, List<FlowTriggerEnum> triggers, Date from, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusInAndCreationDateAfter(
      List<String> workflowIds, List<TaskStatus> statuses, Date from, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateBetween(
      List<String> workflowIds, List<TaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Date from, Date to, Pageable page);

  Page<ActivityEntity> findByTriggerInAndCreationDateBetween(
      List<FlowTriggerEnum> triggers, Date from, Date to, Pageable page);

  Page<ActivityEntity> findByStatusInAndCreationDateBetween(
      List<TaskStatus> statuses, Date from, Date to, Pageable page);

  Page<ActivityEntity> findByStatusInAndTriggerInAndCreationDateBetween(
      List<TaskStatus> statuses, List<FlowTriggerEnum> triggers, Date from, Date to,
      Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndCreationDateBetween(
      List<String> workflowIds, Date from, Date to, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndTriggerInAndCreationDateBetween(
      List<String> workflowIds, List<FlowTriggerEnum> triggers, Date from, Date to, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusInAndCreationDateBetween(
      List<String> workflowIds, List<TaskStatus> statuses, Date from, Date to, Pageable page);

  Page<ActivityEntity> findByCreationDateAfter(Date from, Pageable page);

  Page<ActivityEntity> findByCreationDateBetween(Date from, Date to, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateBefore(
      List<String> workflowIds, List<TaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Date to, Pageable page);

  Page<ActivityEntity> findByTriggerInAndCreationDateBefore(
      List<FlowTriggerEnum> triggers, Date to, Pageable page);

  Page<ActivityEntity> findByStatusInAndCreationDateBefore(
      List<TaskStatus> statuses, Date to, Pageable page);

  Page<ActivityEntity> findByStatusInAndTriggerInAndCreationDateBefore(
      List<TaskStatus> statuses, List<FlowTriggerEnum> triggers, Date to, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndCreationDateBefore(List<String> workflowIds,
      Date to, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndTriggerInAndCreationDateBefore(
      List<String> workflowIds, List<FlowTriggerEnum> triggers, Date to, Pageable page);

  Page<ActivityEntity> findByWorkflowIdInAndStatusInAndCreationDateBefore(
      List<String> workflowIds, List<TaskStatus> statuses, Date to, Pageable page);

  Page<ActivityEntity> findByCreationDateBefore(Date to, Pageable page);
  
  List<ActivityEntity> findByWorkflowIdInAndStatus(List<String> workflowIds, TaskStatus status);

  @Query("{'workflowId' : ?0, 'properties.key' : ?1, 'properties.value' : ?2}")
  ActivityEntity findByWorkflowAndProperty(String workflowId, String key, String value);
}
