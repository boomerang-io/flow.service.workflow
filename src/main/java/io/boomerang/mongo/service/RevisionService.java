package io.boomerang.mongo.service;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.mongo.entity.RevisionEntity;

public interface RevisionService {

  void deleteWorkflow(RevisionEntity flowWorkflowVersionEntity);

  Optional<RevisionEntity> getRevision(String id);
  
  RevisionEntity getLatestWorkflowVersion(String workflowId);

  RevisionEntity getLatestWorkflowVersion(String workflowId, long version);

  long getWorkflowCount(String workFlowId);

  RevisionEntity getWorkflowlWithId(String id);

  RevisionEntity insertWorkflow(RevisionEntity flowWorkflowVersionEntity);

  RevisionEntity updateWorkflow(RevisionEntity flowWorkflowVersionEntity);

  Page<RevisionEntity> getAllWorkflowVersions(Optional<String> workFlowId,
      Pageable pageable);

  RevisionEntity findRevisionTaskProperty(String workflowId, long workflowVersion, String taskId,
      String propertyKey);
}
