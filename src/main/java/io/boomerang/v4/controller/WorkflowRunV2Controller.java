package io.boomerang.v4.controller;

import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
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
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.v4.data.entity.ref.WorkflowRunEntity;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunRequest;
import io.boomerang.v4.model.ref.WorkflowRunSubmitRequest;
import io.boomerang.v4.service.TaskRunService;
import io.boomerang.v4.service.WorkflowRunService;
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
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Search for WorkflowRuns")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<WorkflowRunEntity> queryWorkflowRuns(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status",
      description = "List of statuses to filter for. Defaults to all.", example = "succeeded,skipped",
      required = false) @RequestParam(required = false)  Optional<List<String>> status,
      @Parameter(name = "phase",
      description = "List of phases to filter for. Defaults to all.", example = "completed,finalized",
      required = false) @RequestParam(required = false)  Optional<List<String>> phase,
      @Parameter(name = "teams", description = "List of teams to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
      @Parameter(name = "workflowruns", description = "List of WorkflowRun IDs to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflowruns,
      @Parameter(name = "workflows", description = "List of Workflow IDs to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    return workflowRunService.query(page, limit, sort, fromDate, toDate, labels, status, phase, teams, workflowruns, workflows);
  }

  @PostMapping(value = "/submit")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Submit a Workflow to be run. Will queue the WorkflowRun ready for execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> submitWorkflowRun(
      @Parameter(name = "start",
      description = "Start the WorkflowRun immediately after submission",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean start,
      @RequestBody WorkflowRunSubmitRequest request) {
    return workflowRunService.submit(request, start);
  }

  @GetMapping(value = "/{workflowRunId}")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Retrieve a specific WorkflowRun.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> getTaskRuns(
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun",
      required = true) @PathVariable String workflowRunId,
      @Parameter(name = "withTasks",
      description = "Include Task Runs in the response",
      required = false) @RequestParam(defaultValue="true") boolean withTasks) {
    return workflowRunService.get(workflowRunId, withTasks);
  }

  @PutMapping(value = "/{workflowRunId}/start")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
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
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
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
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
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
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
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
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
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
