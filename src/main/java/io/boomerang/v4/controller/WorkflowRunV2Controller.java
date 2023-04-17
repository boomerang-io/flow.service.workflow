package io.boomerang.v4.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.v4.data.entity.ref.WorkflowRunEntity;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunRequest;
import io.boomerang.v4.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/workflow")
@Tag(name = "WorkflowRun Activity Management",
    description = "Submit requests to execute workflows and provide the ability to search and retrieve workflow activities.")
public class WorkflowRunV2Controller {

  @Autowired
  private WorkflowRunService workflowRunService;

  @GetMapping(value = "/run/query")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Search for Workflow Runs")
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
      @Parameter(name = "workflowRuns", description = "List of workflowRuns to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflowRuns,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    return workflowRunService.query(page, limit, sort, labels, status, phase, teams, workflowRuns);
  }

  @GetMapping(value = "/run/{workflowRunId}")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Retrieve a specific Workflow Run.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> getTaskRuns(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run",
      required = true) @PathVariable String workflowRunId,
      @Parameter(name = "withTasks",
      description = "Include Task Runs in the response",
      required = false) @RequestParam(defaultValue="true") boolean withTasks) {
    return workflowRunService.get(workflowRunId, withTasks);
  }

  @PostMapping(value = "/{workflowId}/run/submit")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Submit a Workflow to be run. Will queue the Workflow Run ready for execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> submitWorkflowRun(
      @Parameter(name = "workflowId",
      description = "ID of Workflow to Request a Run for",
      required = true) @PathVariable(required = true) String workflowId,
      @Parameter(name = "version",
      description = "Workflow Version",
      required = false) @RequestParam(required = false) Optional<Integer> version,
      @Parameter(name = "start",
      description = "Start the Workflow Run immediately after submission",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean start,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.submit(workflowId, version, start, runRequest);
  }

  @PutMapping(value = "/run/{workflowRunId}/start")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Start Workflow Run execution. The Workflow Run has to already have been queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> startWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Start",
      required = true) @PathVariable(required = true) String workflowRunId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.start(workflowRunId, runRequest);
  }

  @PutMapping(value = "/run/{workflowRunId}/finalize")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "End a Workflow Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> endWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Finalize",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.finalize(workflowRunId);
  }

  @DeleteMapping(value = "/run/{workflowRunId}/cancel")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Cancel a Workflow Run")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> cancelWorkflowRun(
      @Parameter(name = "workflowRunId",
      description = "ID of Workflow Run to Cancel",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.cancel(workflowRunId);
  }

  @PutMapping(value = "/run/{workflowRunId}/retry")
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
}
