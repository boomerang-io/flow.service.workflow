package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort.Direction;
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
import io.boomerang.client.WorkflowResponsePage;
import io.boomerang.model.WorkflowCanvas;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowSubmitRequest;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.security.model.AuthType;
import io.boomerang.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/team/{team}/workflow")
@Tag(name = "Workflow Management", description = "Create, list, and manage your Workflows.")
public class TeamWorkflowV2Controller {

  @Autowired
  private WorkflowService workflowService;

  @GetMapping(value = "/{workflow}")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Retrieve a Workflow", description = "Retrieve a version of the Workflow. Defaults to latest. Optionally without Tasks")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Workflow getWorkflow(
      @Parameter(name = "workflow", description = "Workflow reference",
          required = true) @PathVariable String workflow,
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "version", description = "Workflow Version",
          required = false) @RequestParam(required = false) Optional<Integer> version,
      @Parameter(name = "withTasks", description = "Include Workflow Tasks",
      required = false) @RequestParam(defaultValue="true") boolean withTasks) {
    return workflowService.get(team, workflow, version, withTasks);
  }

  @GetMapping(value = "/query")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Search for Workflows")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowResponsePage queryWorkflows(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses", description = "List of statuses to filter for. Defaults to all.",
          example = "active,inactive",
          required = false) @RequestParam(required = false) Optional<List<String>> statuses,
      @Parameter(name = "workflows", description = "List of workflows to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "sort", description = "Ascending (ASC) or Descending (DESC) sort on creationDate", example = "ASC",
  required = true) @RequestParam(defaultValue = "ASC") Optional<Direction> sort) {
    return workflowService.query(team, limit, page, sort, labels, statuses, workflows);
  }

  @PostMapping(value = "")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Create a new workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Workflow createWorkflow(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
    @RequestBody Workflow workflow) {
    return workflowService.create(team, workflow);
  }

  @PutMapping(value = "")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Update, replace, or create new, Workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Workflow applyWorkflow(@RequestBody Workflow workflow,
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "replace", description = "Replace existing version",
          required = false) @RequestParam(required = false, defaultValue = "false") boolean replace) {
    return workflowService.apply(team, workflow, replace);
  }
  
  @GetMapping(value = "/{workflow}/changelog")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Retrieve the changlog", description = "Retrieves each versions changelog and returns them all as a list.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<List<ChangeLogVersion>> getChangelog(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflow", description = "Workflow reference",
          required = true) @PathVariable String workflow) {
    return workflowService.changelog(team, workflow);
  }

  @DeleteMapping(value = "/{workflow}")
  @AuthScope(action = PermissionAction.DELETE, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Delete a workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void deleteWorkflow(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflow",
      description = "Workflow reference", required = true) @PathVariable String workflow) {
    workflowService.delete(team, workflow);
  }

  @PostMapping(value = "/{workflow}/submit")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Submit a Workflow to be run. Will queue the WorkflowRun ready for execution.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowRun submitWorkflow(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflow",
      description = "Workflow reference", required = true) @PathVariable String workflow,
      @Parameter(name = "start",
      description = "Start the WorkflowRun immediately after submission",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean start,
      @RequestBody WorkflowSubmitRequest request) {
    return workflowService.submit(team, workflow, request, start);
  }

  @GetMapping(value = "/{workflow}/export", produces = "application/json")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Export the Workflow as JSON.")
  public ResponseEntity<InputStreamResource> export(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflow",
      description = "Workflow reference", required = true) @PathVariable String workflow) {
    return workflowService.export(team, workflow);
  }

  @GetMapping(value = "/{workflow}/compose")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Convert workflow to compose model for UI Designer and detailed Activity screens.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowCanvas compose(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflow", description = "Workflow reference",
          required = true) @PathVariable String workflow,
      @Parameter(name = "version", description = "Workflow Version",
          required = false) @RequestParam(required = false) Optional<Integer> version) {
    return workflowService.composeGet(team, workflow, version);
  }

  @PutMapping(value = "/{workflow}/compose")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Update, replace, or create new, Workflow for Canvas")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowCanvas applyCanvas(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @RequestBody WorkflowCanvas canvas,
      @Parameter(name = "replace", description = "Replace existing version",
          required = false) @RequestParam(required = false, defaultValue = "false") boolean replace) {
    return workflowService.composeApply(team, canvas, replace);
  }

  @PostMapping(value = "/{workflow}/duplicate")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Duplicates the workflow.")
  public Workflow duplicateWorkflow(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflow", description = "Workflow reference",
      required = true) @PathVariable String workflow) {
    return workflowService.duplicate(team, workflow);
  }

//  @PostMapping(value = "{id}/token")
//  public GenerateTokenResponse createToken(@PathVariable String id, @RequestParam String label) {
//    return workflowService.generateTriggerToken(id, label);
//  }
//
//  @DeleteMapping(value = "{id}/token")
//  public void deleteToken(@PathVariable String id, @RequestParam String label) {
//    workflowService.deleteToken(id, label);
//  }

//  @PostMapping(value = "/workflow/{id}/validateToken", consumes = "application/json; charset=utf-8")
//  public ResponseEntity<HttpStatus> validateToken(@PathVariable String id,
//      @RequestBody GenerateTokenResponse tokenPayload) {
//    return workflowService.validateWorkflowToken(id, tokenPayload);
//  }

  @GetMapping(value = "/{workflow}/available-parameters")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.WORKFLOW, types = {AuthType.team})
  @Operation(summary = "Retrieve the parameters.")
  public List<String> getAvailableParameters(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflow", description = "Workflow reference",
      required = true) @PathVariable String workflow) {
    return workflowService.getAvailableParameters(team, workflow);
  }

//  @GetMapping(value = "/{workflowId}/schedules")
//  public List<WorkflowSchedule> getSchedulesForWorkflow(@PathVariable String workflowId) {
//    return workflowScheduleService.getSchedulesForWorkflow(workflowId);
//  }
//
//  @GetMapping(value = "/{workflowId}/schedules/calendar")
//  public List<WorkflowScheduleCalendar> getCalendarsForWorkflow(@PathVariable String workflowId,
//      @RequestParam Long fromDate, @RequestParam Long toDate) {
//    if (workflowId != null && fromDate != null && toDate != null) {
//      Date from = new Date(fromDate * 1000);
//      Date to = new Date(toDate * 1000);
//      return workflowScheduleService.getCalendarsForWorkflow(workflowId, from, to);
//    } else {
//      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
//    }
//  }
}
