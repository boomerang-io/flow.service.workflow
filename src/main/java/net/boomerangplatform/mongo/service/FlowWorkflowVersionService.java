package net.boomerangplatform.mongo.service;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;

public interface FlowWorkflowVersionService {

  void deleteWorkflow(FlowWorkflowRevisionEntity flowWorkflowVersionEntity);

  FlowWorkflowRevisionEntity getLatestWorkflowVersion(String workflowId);

  FlowWorkflowRevisionEntity getLatestWorkflowVersion(String workflowId, long version);

  long getWorkflowCount(String workFlowId);

  FlowWorkflowRevisionEntity getWorkflowlWithId(String id);

  FlowWorkflowRevisionEntity insertWorkflow(FlowWorkflowRevisionEntity flowWorkflowVersionEntity);

  FlowWorkflowRevisionEntity updateWorkflow(FlowWorkflowRevisionEntity flowWorkflowVersionEntity);

  Page<FlowWorkflowRevisionEntity> getAllWorkflowVersions(Optional<String> workFlowId,
      Pageable pageable);
}
