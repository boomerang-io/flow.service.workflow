package io.boomerang.mongo.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import io.boomerang.v4.data.entity.WorkflowScheduleEntity;
import io.boomerang.v4.model.enums.WorkflowScheduleStatus;

public interface FlowWorkflowScheduleRepository
    extends MongoRepository<WorkflowScheduleEntity, String> {

  @Query(value = "{'creationDate':{ $lt: ?1, $gte: ?0}}")
  List<WorkflowScheduleEntity> findAll(Date fromDate, Date toDate);

  List<WorkflowScheduleEntity> findByWorkflowId(String workflowId);

  List<WorkflowScheduleEntity> findByIdIn(List<String> ids);

  List<WorkflowScheduleEntity> findByIdInAndStatusIn(List<String> ids, List<WorkflowScheduleStatus> statuses);

  List<WorkflowScheduleEntity> findByWorkflowIdAndStatusIn(
      String workflowId, List<WorkflowScheduleStatus> statuses);
  
  List<WorkflowScheduleEntity> findByWorkflowIdAndStatus(String workflowId, WorkflowScheduleStatus status);
}
