package io.boomerang.client;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.TaskRun;
import io.boomerang.model.ref.TaskRunEndRequest;
import io.boomerang.model.ref.TaskTemplate;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowCount;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunCount;
import io.boomerang.model.ref.WorkflowRunInsight;
import io.boomerang.model.ref.WorkflowRunRequest;
import io.boomerang.model.ref.WorkflowSubmitRequest;
import io.boomerang.model.ref.WorkflowTemplate;

public interface EngineClient {

  WorkflowRun getWorkflowRun(String workflowRunId, boolean withTasks);

  WorkflowRunResponsePage queryWorkflowRuns(Optional<Long> fromDate, Optional<Long> toDate,
      Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryPhase, Optional<List<String>> queryWorkflowRuns,
      Optional<List<String>> queryWorkflows, Optional<List<String>> queryTriggers);

  WorkflowRunInsight insightWorkflowRuns(Optional<List<String>> queryLabels,
      Optional<List<String>> queryWorkflowRuns, Optional<List<String>> queryWorkflows,
      Optional<Long> fromDate, Optional<Long> toDate);

  WorkflowRunCount countWorkflowRuns(Optional<List<String>> queryLabels,
      Optional<List<String>> queryWorkflows, Optional<Long> fromDate, Optional<Long> toDate);

  WorkflowRun startWorkflowRun(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest);

  WorkflowRun finalizeWorkflowRun(String workflowRunId);

  WorkflowRun cancelWorkflowRun(String workflowRunId);

  WorkflowRun retryWorkflowRun(String workflowRunId);

  void deleteWorkflowRun(String workflowRunId);

  Workflow getWorkflow(String workflowId, Optional<Integer> version, boolean withTasks);

  WorkflowResponsePage queryWorkflows(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryIds);

  Workflow createWorkflow(Workflow workflow);

  Workflow applyWorkflow(Workflow workflow, boolean replace);

  WorkflowRun submitWorkflow(String workflowId, WorkflowSubmitRequest request, boolean start);

  void enableWorkflow(String workflowId);

  void disableWorkflow(String workflowId);

  void deleteWorkflow(String workflowId);

  TaskRun endTaskRun(String taskRunId, TaskRunEndRequest request);

  TaskRun getTaskRun(String taskRunId);
  
  StreamingResponseBody streamTaskRunLog(String taskRunId);

  TaskTemplate getTaskTemplate(String name, Optional<Integer> version);

  TaskTemplateResponsePage queryTaskTemplates(Optional<Integer> queryLimit,
      Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      List<String> queryRefs);

  TaskTemplate createTaskTemplate(TaskTemplate taskTemplate);

  TaskTemplate applyTaskTemplate(TaskTemplate workflow, boolean replace);

  List<ChangeLogVersion> getTaskTemplateChangeLog(String name);

  ResponseEntity<Void> deleteTaskTemplate(String name);

  WorkflowTemplate getWorkflowTemplate(String name, Optional<Integer> version, boolean withTasks);

  WorkflowTemplateResponsePage queryWorkflowTemplates(Optional<Integer> queryLimit,
      Optional<Integer> queryPage, Optional<Direction> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryNames);

  WorkflowTemplate createWorkflowTemplate(WorkflowTemplate workflow);

  WorkflowTemplate applyWorkflowTemplate(WorkflowTemplate workflow, boolean replace);

  ResponseEntity<Void> deleteWorkflowTemplate(String name);

  List<ChangeLogVersion> getWorkflowChangeLog(String workflowId);

  WorkflowCount countWorkflows(Optional<List<String>> queryLabels,
      Optional<List<String>> queryWorkflows, Optional<Long> fromDate, Optional<Long> toDate);
}
