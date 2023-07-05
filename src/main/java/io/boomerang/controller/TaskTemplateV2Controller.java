package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.client.TaskTemplateResponsePage;
import io.boomerang.service.TaskTemplateService;
import io.boomerang.tekton.TektonTask;
import io.boomerang.v4.model.ref.TaskTemplate;
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
  
  @GetMapping(value = "{name}", produces = "application/x-yaml")
  @Operation(summary = "Retrieve a specific task template as Tekton Task YAML. If no version specified, the latest version is returned.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TektonTask getTaskTemplateYAML(
      @Parameter(name = "name",
      description = "Name of Task Template",
      required = true) @PathVariable String name,
      @Parameter(name = "version",
      description = "Task Template Version",
      required = false) @RequestParam(required = false) Optional<Integer> version) {
    return taskTemplateService.getAsTekton(name, version);
  }
  
  @GetMapping(value = "/query")
  @Operation(summary = "Search for Task Templates")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskTemplateResponsePage queryTaskTemplates(
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses",
      description = "List of statuses to filter for.", example = "active,inactive",
      required = false) @RequestParam(required = false, defaultValue = "active")  Optional<List<String>> statuses,
      @Parameter(name = "names",
      description = "List of TaskTemplate Names  to filter for. Defaults to all.", example = "switch,event-wait",
      required = false) @RequestParam(required = false)  Optional<List<String>> names,
      @Parameter(name = "teams", description = "List of teams to filter for. If no team is specified then Global task templates will be retrieved.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "sort", description = "Ascending (ASC) or Descending (DESC) sort on creationDate", example = "ASC",
  required = true) @RequestParam(defaultValue = "ASC") Optional<Direction> sort) {
    return taskTemplateService.query(limit, page, sort, labels, statuses, names, teams);
  }

  @PostMapping(value = "")
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

  @PutMapping(value = "")
  @Operation(summary = "Update, replace, or create new, Task Template",
            description = "The name must only contain alphanumeric and - characeters. If the name exists, apply will create a new version.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TaskTemplate> applyTaskTemplate(@RequestBody TaskTemplate taskTemplate,
      @Parameter(name = "replace",
      description = "Replace existing version",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean replace,
      @Parameter(name = "team", description = "Team as owner reference.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false) Optional<String> team) {
    return taskTemplateService.apply(taskTemplate, replace, team);
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

  //TODO determine if the consumes is enough to direct it here.
  @PostMapping(value = "", consumes = "application/x-yaml", produces = "application/x-yaml")
  @Operation(summary = "Create a new Task Template using Tekton Task YAML",
            description = "The name needs to be unique and must only contain alphanumeric and - characeters.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TektonTask createTaskTemplateYAML(
      @Parameter(name = "team", description = "Team as owner reference.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false) Optional<String> team,
      @RequestBody TektonTask taskTemplate) {
    return taskTemplateService.createAsTekton(taskTemplate, team);
  }

//TODO determine if the consumes is enough to direct it here.
  @PutMapping(value = "", consumes = "application/x-yaml", produces = "application/x-yaml")
  @Operation(summary = "Update, replace, or create new, Task Template",
            description = "The name must only contain alphanumeric and - characeters. If the name exists, apply will create a new version.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TektonTask applyTaskTemplateYAML(@RequestBody TektonTask taskTemplate,
      @Parameter(name = "replace",
      description = "Replace existing version",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean replace,
      @Parameter(name = "team", description = "Team as owner reference.", example = "63d3656ca845957db7d25ef0,63a3e732b0496509a7f1d763",
      required = false) @RequestParam(required = false) Optional<String> team) {
    return taskTemplateService.applyAsTekton(taskTemplate, replace, team);
  }

//TODO determine if the consumes is enough to direct it here.
  @PostMapping(value = "validate", consumes = "application/x-yaml", produces = "application/x-yaml")
  public void validateYaml(@RequestBody TektonTask tektonTask) {
    taskTemplateService.validateAsTekton(tektonTask);
  }
}
