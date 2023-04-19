package io.boomerang.v4.client;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import io.boomerang.v4.model.ref.TaskRun;
import io.boomerang.v4.model.ref.TaskRunEndRequest;
import io.boomerang.v4.model.ref.Workflow;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunInsight;
import io.boomerang.v4.model.ref.WorkflowRunRequest;
import io.boomerang.v4.model.ref.WorkflowRunSubmitRequest;

public interface EngineClient {

  WorkflowRun getWorkflowRun(String workflowRunId, boolean withTasks);

  WorkflowRunResponsePage queryWorkflowRuns(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase,
      Optional<List<String>> queryIds);

  WorkflowRunInsight insightWorkflowRuns(Optional<List<String>> queryLabels,
      Optional<List<String>> queryIds, Optional<Long> fromDate, Optional<Long> toDate);

  WorkflowRun submitWorkflowRun(WorkflowRunSubmitRequest request, boolean start);

  WorkflowRun startWorkflowRun(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest);

  WorkflowRun finalizeWorkflowRun(String workflowRunId);

  WorkflowRun cancelWorkflowRun(String workflowRunId);

  WorkflowRun retryWorkflowRun(String workflowRunId);

  Workflow getWorkflow(String workflowId, Optional<Integer> version, boolean withTasks);

  WorkflowResponsePage queryWorkflows(int page, int limit, Sort sort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryIds);

  Workflow createWorkflow(Workflow workflow);

  Workflow applyWorkflow(Workflow workflow, boolean replace);

  void enableWorkflow(String workflowId);

  void disableWorkflow(String workflowId);

  void deleteWorkflow(String workflowId);

  TaskRun endTaskRun(String taskRunId, TaskRunEndRequest request);

  TaskRun getTaskRun(String taskRunId);
}
