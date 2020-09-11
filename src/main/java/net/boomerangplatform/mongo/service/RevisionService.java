package net.boomerangplatform.mongo.service;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import net.boomerangplatform.mongo.entity.RevisionEntity;

public interface RevisionService {

  void deleteWorkflow(RevisionEntity flowWorkflowVersionEntity);

  RevisionEntity getLatestWorkflowVersion(String workflowId);

  RevisionEntity getLatestWorkflowVersion(String workflowId, long version);

  long getWorkflowCount(String workFlowId);

  RevisionEntity getWorkflowlWithId(String id);

  RevisionEntity insertWorkflow(RevisionEntity flowWorkflowVersionEntity);

  RevisionEntity updateWorkflow(RevisionEntity flowWorkflowVersionEntity);

  Page<RevisionEntity> getAllWorkflowVersions(Optional<String> workFlowId,
      Pageable pageable);
}
