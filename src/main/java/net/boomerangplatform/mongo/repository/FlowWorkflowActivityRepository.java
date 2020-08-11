package net.boomerangplatform.mongo.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;

public interface FlowWorkflowActivityRepository
    extends MongoRepository<FlowWorkflowActivityEntity, String> {

  @Query(value = "{'creationDate':{ $lt: ?1, $gte: ?0}}")
  Page<FlowWorkflowActivityEntity> findAll(Date fromDate, Date toDate, Pageable pageable);

  Page<FlowWorkflowActivityEntity> findByworkflowId(String workflowId, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusInAndTriggerIn(
      List<String> workflowIds, List<FlowTaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Pageable page);

  Page<FlowWorkflowActivityEntity> findByTriggerIn(List<FlowTriggerEnum> triggers, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusIn(List<FlowTaskStatus> statuses, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusInAndTriggerIn(List<FlowTaskStatus> statuses,
      List<FlowTriggerEnum> triggers, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdIn(List<String> workflowIds, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndTriggerIn(List<String> workflowIds,
      List<FlowTriggerEnum> triggers, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusIn(List<String> workflowIds,
      List<FlowTaskStatus> statuses, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateAfter(
      List<String> workflowIds, List<FlowTaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Date from, Pageable page);

  Page<FlowWorkflowActivityEntity> findByTriggerInAndCreationDateAfter(
      List<FlowTriggerEnum> triggers, Date from, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusInAndCreationDateAfter(List<FlowTaskStatus> statuses,
      Date date, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusInAndTriggerInAndCreationDateAfter(
      List<FlowTaskStatus> statuses, List<FlowTriggerEnum> triggers, Date date, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndCreationDateAfter(List<String> workflowIds,
      Date from, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndTriggerInAndCreationDateAfter(
      List<String> workflowIds, List<FlowTriggerEnum> triggers, Date from, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusInAndCreationDateAfter(
      List<String> workflowIds, List<FlowTaskStatus> statuses, Date from, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateBetween(
      List<String> workflowIds, List<FlowTaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Date from, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByTriggerInAndCreationDateBetween(
      List<FlowTriggerEnum> triggers, Date from, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusInAndCreationDateBetween(
      List<FlowTaskStatus> statuses, Date from, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusInAndTriggerInAndCreationDateBetween(
      List<FlowTaskStatus> statuses, List<FlowTriggerEnum> triggers, Date from, Date to,
      Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndCreationDateBetween(
      List<String> workflowIds, Date from, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndTriggerInAndCreationDateBetween(
      List<String> workflowIds, List<FlowTriggerEnum> triggers, Date from, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusInAndCreationDateBetween(
      List<String> workflowIds, List<FlowTaskStatus> statuses, Date from, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByCreationDateAfter(Date from, Pageable page);

  Page<FlowWorkflowActivityEntity> findByCreationDateBetween(Date from, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusInAndTriggerInAndCreationDateBefore(
      List<String> workflowIds, List<FlowTaskStatus> statuses, List<FlowTriggerEnum> triggers,
      Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByTriggerInAndCreationDateBefore(
      List<FlowTriggerEnum> triggers, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusInAndCreationDateBefore(
      List<FlowTaskStatus> statuses, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByStatusInAndTriggerInAndCreationDateBefore(
      List<FlowTaskStatus> statuses, List<FlowTriggerEnum> triggers, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndCreationDateBefore(List<String> workflowIds,
      Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndTriggerInAndCreationDateBefore(
      List<String> workflowIds, List<FlowTriggerEnum> triggers, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatusInAndCreationDateBefore(
      List<String> workflowIds, List<FlowTaskStatus> statuses, Date to, Pageable page);

  Page<FlowWorkflowActivityEntity> findByCreationDateBefore(Date to, Pageable page);
  
  List<FlowWorkflowActivityEntity> findByWorkflowIdInAndStatus(List<String> workflowIds, FlowTaskStatus status);

  @Query("{'workflowId' : ?0, 'properties.key' : ?1, 'properties.value' : ?2}")
  FlowWorkflowActivityEntity findByWorkflowAndProperty(String workflowId, String key, String value);
}
