package io.boomerang.v4.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.WorkflowScheduleEntity;
import io.boomerang.v4.model.enums.WorkflowScheduleStatus;

public interface WorkflowScheduleRepository extends MongoRepository<WorkflowScheduleEntity, String> {
  
  Optional<List<WorkflowScheduleEntity>> findByWorkflowRef(String ref);
  
  Optional<List<WorkflowScheduleEntity>> findByIdInAndStatusIn(List<String> ids, List<WorkflowScheduleStatus> statuses);
  
  Optional<List<WorkflowScheduleEntity>> findByWorkflowRefInAndStatusIn(List<String> workflowRefs, List<WorkflowScheduleStatus> statuses);

}
