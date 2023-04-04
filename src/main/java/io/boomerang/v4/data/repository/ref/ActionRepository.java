package io.boomerang.v4.data.repository.ref;

import java.util.Date;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.ref.ActionEntity;
import io.boomerang.v4.model.enums.ref.ActionStatus;
import io.boomerang.v4.model.enums.ref.ActionType;

public interface ActionRepository extends MongoRepository<ActionEntity, String> {

  ActionEntity findByTaskRunRef(String taskRunRef);

  long countByWorkflowRunRefAndStatus(String workflowRunRef, ActionStatus status);
  
  long countByCreationDateBetween(Date from, Date to);
  
  long countByTypeAndCreationDateBetween(ActionType type, Date from, Date to);

  long countByType(ActionType type);

  long countByStatus(ActionStatus submitted);

  long countByStatusAndCreationDateBetween(ActionStatus submitted, Date date, Date date2);

  long countByStatusAndTypeAndCreationDateBetween(ActionStatus submitted, ActionType type,
      Date date, Date date2);

  long countByStatusAndType(ActionStatus submitted, ActionType type);
}

