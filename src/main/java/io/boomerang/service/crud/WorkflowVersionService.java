package io.boomerang.service.crud;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import io.boomerang.model.FlowWorkflowRevision;
import io.boomerang.model.RevisionResponse;

public interface WorkflowVersionService {

  void deleteWorkflowVersionWithId(String id);

  long getLatestWorkflowVersionCount(String workflowId);

  FlowWorkflowRevision getWorkflowVersion(String workflowId, long verison);

  FlowWorkflowRevision getWorkflowVersionWithId(String id);

  FlowWorkflowRevision insertWorkflowVersion(FlowWorkflowRevision flowWorkflowEntity);

  List<RevisionResponse> viewChangelog(Optional<String> workFlowId, Pageable pageable);

  FlowWorkflowRevision getLatestWorkflowVersion(String workflowId, boolean triggeredByJob);

}
