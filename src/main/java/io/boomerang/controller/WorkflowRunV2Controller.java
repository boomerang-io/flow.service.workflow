package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunCount;
import io.boomerang.model.ref.WorkflowRunRequest;
import io.boomerang.model.ref.WorkflowSubmitRequest;
import io.boomerang.service.TaskRunService;
import io.boomerang.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/workflowrun")
@Tag(name = "WorkflowRun Management",
    description = "Submit requests to execute Workflows and provide the ability to search and retrieve Workflow activities.")
public class WorkflowRunV2Controller {

  @Autowired
  private WorkflowRunService workflowRunService;

  @Autowired
  private TaskRunService taskRunService;

  @GetMapping(value = "/query")
  @Operation(summary = "Search for WorkflowRuns")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<WorkflowRun> query(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses",
      description = "List of statuses to filter for. Defaults to all.", example = "succeeded,skipped",
      required = false) @RequestParam(required = false)  Optional<List<String>> statuses,
      @Parameter(name = "phase",
      description = "List of phases to filter for. Defaults to all.", example = "completed,finalized",
      required = false) @RequestParam(required = false)  Optional<List<String>> phase,
      @Parameter(name = "teams", description = "List of teams to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
      @Parameter(name = "workflowruns", description = "List of WorkflowRun IDs to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflowruns,
      @Parameter(name = "workflows", description = "List of Workflow IDs to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "triggers", description = "List of Triggers to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> triggers,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "order", description = "Ascending (ASC) or Descending (DESC) sort order on creationDate", example = "ASC",
  required = true) @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {
    return workflowRunService.query(fromDate, toDate, limit, page, order, labels, statuses, phase, teams, workflowruns, workflows, triggers);
  }  

  @GetMapping(value = "/count")
  @Operation(summary = "Retrieve a summary of WorkflowRuns by Status.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowRunCount count(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "teams", description = "List of teams to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
      @Parameter(name = "workflows",
      description = "List of Workflow IDs  to filter for. Does not validate the IDs provided. Defaults to all.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false)  Optional<List<String>> workflows,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {
    return workflowRunService.count(fromDate, toDate, labels, teams, workflows);
  }

  @GetMapping(value = "/{workflowRunId}")
  @Operation(summary = "Retrieve a specific WorkflowRun.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> getTaskRuns(
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun",
      required = true) @PathVariable String workflowRunId,
      @Parameter(name = "withTasks",
      description = "Include Task Runs in the response",
      required = false) @RequestParam(defaultValue = "true") boolean withTasks) {
    return workflowRunService.get(workflowRunId, withTasks);
  }

  @PutMapping(value = "/{workflowRunId}/start")
  @Operation(summary = "Start Workflow Run execution. The Workflow Run has to already have been queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> startWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun to Start",
      required = true) @PathVariable(required = true) String workflowRunId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.start(workflowRunId, runRequest);
  }

  @PutMapping(value = "/{workflowRunId}/finalize")
  @Operation(summary = "End a WorkflowRun")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> endWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Finalize",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.finalize(workflowRunId);
  }

  @DeleteMapping(value = "/{workflowRunId}/cancel")
  @Operation(summary = "Cancel a WorkflowRun")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> cancelWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Cancel",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.cancel(workflowRunId);
  }

  @PutMapping(value = "/{workflowRunId}/retry")
  @Operation(summary = "Retry Workflow Run execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> retryWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Retry.",
      required = true) @PathVariable(required = true) String workflowRunId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.retry(workflowRunId);
  }

  @GetMapping(value = "/{workflowRunId}/{taskRunId}/log")
  @Operation(summary = "Retrieve a TaskRuns log from a specific WorkflowRun.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  @ResponseBody
  public ResponseEntity<StreamingResponseBody> getTaskRunLog(
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun",
      required = true) @PathVariable String workflowRunId,
      @Parameter(name = "taskRunId",
      description = "Id of TaskRun",
      required = true) @PathVariable String taskRunId,
      HttpServletResponse response) {
  response.setContentType("text/plain");
  response.setCharacterEncoding("UTF-8");
  return new ResponseEntity<StreamingResponseBody>(taskRunService.getTaskRunLog(workflowRunId, taskRunId), HttpStatus.OK);
  }
}
