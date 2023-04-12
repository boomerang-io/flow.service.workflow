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
import io.boomerang.v4.data.entity.ref.WorkflowEntity;
import io.boomerang.v4.model.WorkflowCanvas;
import io.boomerang.v4.model.enums.WorkflowScope;
import io.boomerang.v4.model.ref.Workflow;
import io.boomerang.v4.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/workflow")
@Tag(name = "Workflow Management", description = "Create, List, and Manage your workflows.")
public class WorkflowV2Controller {

  @Autowired
  private WorkflowService workflowService;

  @GetMapping(value = "/{workflowId}")
  @Operation(summary = "Retrieve a version of the workflow. Defaults to latest.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Workflow> getWorkflow(
      @Parameter(name = "workflowId", description = "ID of Workflow",
          required = true) @PathVariable String workflowId,
      @Parameter(name = "version", description = "Workflow Version",
          required = false) @RequestParam(required = false) Optional<Integer> version,
      @Parameter(name = "withTasks", description = "Include Workflow Tasks",
      required = false) @RequestParam(defaultValue="true") boolean withTasks) {
    return workflowService.get(workflowId, version, withTasks);
  }

  @GetMapping(value = "/query")
  @Operation(summary = "Search for Workflows")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<WorkflowEntity> queryWorkflows(@Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status", description = "List of statuses to filter for. Defaults to all.",
          example = "active,archived",
          required = false) @RequestParam(required = false) Optional<List<String>> status,
      @Parameter(name = "scope", description = "The level of scope to filter to.", example = "global, template, team, or user", 
      required = false) @RequestParam(required = false) Optional<WorkflowScope> scope,
      @Parameter(name = "refs", description = "List of ids to filter for. Combined with scope.", 
      required = false) @RequestParam(required = false) Optional<List<String>> refs,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    return workflowService.query(page, limit, sort, labels, status, scope, refs);
  }

  @PostMapping(value = "/")
  @Operation(summary = "Create a new workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Workflow> createWorkflow(
      @Parameter(name = "scope", description = "The level of scope to apply to the Workflow.", example = "global, template, team, or user", 
      required = false) @RequestParam(required = false) Optional<WorkflowScope> scope,
    @Parameter(name = "owner", description = "Owner reference. Only relevant if scope = team|user", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
        required = false) @RequestParam(required = false) Optional<String> owner,
    @RequestBody Workflow workflow) {
    return workflowService.create(workflow, scope, owner);
  }

  @PutMapping(value = "/")
  @Operation(summary = "Update, replace, or create new, Workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Workflow> applyWorkflow(@RequestBody Workflow workflow,
      @Parameter(name = "replace", description = "Replace existing version",
          required = false) @RequestParam(required = false,
              defaultValue = "false") boolean replace) {
    return workflowService.apply(workflow, replace);
  }

  @GetMapping(value = "/{workflowId}/compose")
  @Operation(summary = "Convert workflow to compose model for UI Designer and detailed Activity screens.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<WorkflowCanvas> composeWorkflow(

      @Parameter(name = "workflowId", description = "ID of Workflow",
          required = true) @PathVariable String workflowId,
      @Parameter(name = "version", description = "Workflow Version",
          required = false) @RequestParam(required = false) Optional<Integer> version) {
    return workflowService.compose(workflowId, version);
  }

  @PutMapping(value = "/{workflowId}/enable")
  @Operation(summary = "Enable a workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> enableWorkflow(@Parameter(name = "workflowId",
      description = "ID of Workflow", required = true) @PathVariable String workflowId) {
    return workflowService.enable(workflowId);
  }

  @PutMapping(value = "/{workflowId}/disable")
  @Operation(summary = "Disable a workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> disableWorkflow(@Parameter(name = "workflowId",
      description = "ID of Workflow", required = true) @PathVariable String workflowId) {
    return workflowService.disable(workflowId);
  }

  @DeleteMapping(value = "/{workflowId}")
  @Operation(summary = "Delete a workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> deleteWorkflow(@Parameter(name = "workflowId",
      description = "ID of Workflow", required = true) @PathVariable String workflowId) {
    return workflowService.delete(workflowId);
  }  
}
