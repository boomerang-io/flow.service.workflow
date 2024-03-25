package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunCount;
import io.boomerang.model.ref.WorkflowRunRequest;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.service.WorkflowRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/team/{team}/workflowrun")
@Tag(name = "WorkflowRun Management",
    description = "Submit requests to execute Workflows and provide the ability to search and retrieve Workflow activities.")
public class TeamWorkflowRunV2Controller {

  @Autowired
  private WorkflowRunService workflowRunService;

  @GetMapping(value = "/query")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOWRUN, types = {AuthType.team})
  @Operation(summary = "Search for WorkflowRuns")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<WorkflowRun> query(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses",
      description = "List of statuses to filter for. Defaults to all.", example = "succeeded,skipped",
      required = false) @RequestParam(required = false)  Optional<List<String>> statuses,
      @Parameter(name = "phase",
      description = "List of phases to filter for. Defaults to all.", example = "completed,finalized",
      required = false) @RequestParam(required = false)  Optional<List<String>> phase,
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
    return workflowRunService.query(team, fromDate, toDate, limit, page, order, labels, statuses, phase, workflowruns, workflows, triggers);
  }  

  @GetMapping(value = "/count")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOWRUN, types = {AuthType.team})
  @Operation(summary = "Retrieve a summary of WorkflowRuns by Status.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowRunCount count(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "workflows",
      description = "List of Workflow IDs  to filter for. Does not validate the IDs provided. Defaults to all.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false)  Optional<List<String>> workflows,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {
    return workflowRunService.count(team, fromDate, toDate, labels, workflows);
  }

  @GetMapping(value = "/{workflowRunId}")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOWRUN, types = {AuthType.team})
  @Operation(summary = "Retrieve a specific WorkflowRun.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> get(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun",
      required = true) @PathVariable String workflowRunId,
      @Parameter(name = "withTasks",
      description = "Include Task Runs in the response",
      required = false) @RequestParam(defaultValue = "true") boolean withTasks) {
    return workflowRunService.get(team, workflowRunId, withTasks);
  }

  @PutMapping(value = "/{workflowRunId}/start")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.WORKFLOWRUN, types = {AuthType.team})
  @Operation(summary = "Start WorkflowRun execution. The WorkflowRun has to already have been queued.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> start(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun to Start",
      required = true) @PathVariable(required = true) String workflowRunId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.start(team, workflowRunId, runRequest);
  }

  @PutMapping(value = "/{workflowRunId}/finalize")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.WORKFLOWRUN, types = {AuthType.team})
  @Operation(summary = "End a WorkflowRun")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> finalize(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun to Finalize",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.finalize(team, workflowRunId);
  }

  @DeleteMapping(value = "/{workflowRunId}/cancel")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.WORKFLOWRUN, types = {AuthType.team})
  @Operation(summary = "Cancel a WorkflowRun")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> cancel(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun to Cancel",
      required = true) @PathVariable(required = true) String workflowRunId) {
    return workflowRunService.cancel(team, workflowRunId);
  }

  @PutMapping(value = "/{workflowRunId}/retry")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.WORKFLOWRUN, types = {AuthType.team})
  @Operation(summary = "Retry WorkflowRun execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowRun> retry(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflowRunId",
      description = "ID of WorkflowRun to Retry.",
      required = true) @PathVariable(required = true) String workflowRunId,
      @RequestBody Optional<WorkflowRunRequest> runRequest) {
    return workflowRunService.retry(team, workflowRunId);
  }
}
