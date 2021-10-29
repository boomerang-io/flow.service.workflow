package io.boomerang.mongo.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.mongo.model.WorkflowScheduleStatus;

public interface FlowWorkflowScheduleRepository
    extends MongoRepository<WorkflowScheduleEntity, String> {

  @Query(value = "{'creationDate':{ $lt: ?1, $gte: ?0}}")
  List<WorkflowScheduleEntity> findAll(Date fromDate, Date toDate);

  List<WorkflowScheduleEntity> findByWorkflowId(String workflowId);
  
  List<ActivityEntity> findByWorkflowIdAndCreationDateBetween(
      String workflowId, Date from, Date to);

  List<WorkflowScheduleEntity> findByWorkflowIdAndStatusIn(
      String workflowId, List<WorkflowScheduleStatus> statuses);
}
