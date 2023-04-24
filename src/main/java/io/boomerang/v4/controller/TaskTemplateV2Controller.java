package io.boomerang.v4.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.FlowTaskTemplate;
import io.boomerang.model.TemplateScope;
import io.boomerang.model.tekton.TektonTask;
import io.boomerang.v4.client.TaskTemplateResponsePage;
import io.boomerang.v4.model.ref.TaskTemplate;
import io.boomerang.v4.service.TaskTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/tasktemplate")
@Tag(name = "Task Template Management",
description = "Create and Manage the Task Templates, or Task Definitions.")
public class TaskTemplateV2Controller {

  @Autowired
  private TaskTemplateService taskTemplateService;
  
  @GetMapping(value = "/{name}")
  @Operation(summary = "Retrieve a specific task template. If no version specified, the latest version is returned.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskTemplate> getTaskTemplateWithId(
      @Parameter(name = "name",
      description = "Name of Task Template",
      required = true) @PathVariable String name,
      @Parameter(name = "version",
      description = "Task Template Version",
      required = false) @RequestParam(required = false) Optional<Integer> version) {
    return taskTemplateService.get(name, version);
  }
  
  @GetMapping(value = "/query")
  @Operation(summary = "Search for Task Templates")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskTemplateResponsePage queryTaskTemplates(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status",
      description = "List of statuses to filter for.", example = "active,inactive",
      required = false) @RequestParam(required = false, defaultValue = "active")  Optional<List<String>> status,
      @Parameter(name = "names",
      description = "List of TaskTemplate Names  to filter for. Defaults to all.", example = "switch,event-wait",
      required = false) @RequestParam(required = false)  Optional<List<String>> names,
      @Parameter(name = "teams", description = "List of teams to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    return taskTemplateService.query(page, limit, sort, labels, status, names, teams);
  }

  @PostMapping(value = "/")
  @Operation(summary = "Create a new Task Template",
            description = "The name needs to be unique and must only contain alphanumeric and - characeters.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskTemplate> createTaskTemplate(
      @Parameter(name = "team", description = "Team as owner reference.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false) Optional<String> team,
      @RequestBody TaskTemplate taskTemplate) {
    return taskTemplateService.create(taskTemplate, team);
  }

  @PutMapping(value = "/")
  @Operation(summary = "Update, replace, or create new, Task Template",
            description = "The name must only contain alphanumeric and - characeters. If the name exists, apply will create a new version.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskTemplate> applyTaskTemplate(@RequestBody TaskTemplate taskTemplate,
      @Parameter(name = "replace",
      description = "Replace existing version",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean replace) {
    return taskTemplateService.apply(taskTemplate, replace);
  }

  @PutMapping(value = "/{name}/enable")
  @Operation(summary = "Enable a TaskTemplate")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "No Content"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void enableWorkflow(
      @Parameter(name = "name",
      description = "Name of Task Template",
      required = true) @PathVariable String name) {
    taskTemplateService.enable(name);
  }

  @PutMapping(value = "/{name}/disable")
  @Operation(summary = "Disable a TaskTemplate")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "No Content"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void disableWorkflow(
      @Parameter(name = "name",
      description = "Name of Task Template",
      required = true) @PathVariable String name) {
    taskTemplateService.disable(name);
  }

  @PostMapping(value = "yaml/validate", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate validateYaml(@RequestBody TektonTask tektonTask) {
    return taskTemplateService.validateTaskTemplate(tektonTask);
  }
  
  @GetMapping(value = "{id}")
  public FlowTaskTemplate getTaskTemplateWithId(@PathVariable String id) {
    return taskTemplateService.getTaskTemplateWithId(id);
  }

  @GetMapping(value = "{id}/yaml",  produces = "application/x-yaml")
  public TektonTask getTaskTemplateYamlWithId(@PathVariable String id) {
    return taskTemplateService.getTaskTemplateYamlWithId(id);
  }
  
  @GetMapping(value = "{id}/yaml/{revision}",  produces = "application/x-yaml")
  public TektonTask getTaskTemplateYamlWithIdandRevision(@PathVariable String id, @PathVariable Integer revision ) {
    return taskTemplateService.getTaskTemplateYamlWithIdAndRevision(id, revision);
  }
  
  @PostMapping(value = "yaml", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate insertTaskTemplateYaml(@RequestBody TektonTask tektonTask, @RequestParam(required = true) TemplateScope scope, @RequestParam(required = false) String teamId) {
    return taskTemplateService.insertTaskTemplateYaml(tektonTask, scope, teamId);
  }
  
  @PutMapping(value = "{id}/yaml", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate updateTaskTemplateWithYaml(@PathVariable String id, @RequestBody TektonTask tektonTask) {
    return taskTemplateService.updateTaskTemplateWithYaml(id, tektonTask);
  }
  
  @PutMapping(value = "{id}/yaml/{revision}", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate updateTaskTemplateWithYamlForRevision(@PathVariable String id, @RequestBody TektonTask tektonTask, @PathVariable Integer revision,  @RequestParam(required = false) String comment) {
    return taskTemplateService.updateTaskTemplateWithYaml(id, tektonTask, revision, comment);
  }
}
