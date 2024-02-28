package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.client.TaskTemplateResponsePage;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.TaskTemplate;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.service.TaskTemplateService;
import io.boomerang.tekton.TektonTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/team/{team}/tasktemplate")
@Tag(name = "Task Template Management",
description = "Create and Manage the global Task Templates, or Task Definitions.")
public class TeamTaskTemplateV2Controller {

  @Autowired
  private TaskTemplateService taskTemplateService;
  
  @GetMapping(value = "/{name}")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Retrieve a specific task template. If no version specified, the latest version is returned.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskTemplate getTaskTemplateWithId(
      @Parameter(name = "name",
      description = "Name of Task Template",
      required = true) @PathVariable String name,
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "version",
      description = "Task Template Version",
      required = false) @RequestParam(required = false) Optional<Integer> version) {
    return taskTemplateService.get(name, version, Optional.of(team));
  }
  
  @GetMapping(value = "{name}", produces = "application/x-yaml")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Retrieve a specific task template as Tekton Task YAML. If no version specified, the latest version is returned.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TektonTask getTaskTemplateYAML(
      @Parameter(name = "name",
      description = "Name of Task Template",
      required = true) @PathVariable String name,
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "version",
      description = "Task Template Version",
      required = false) @RequestParam(required = false) Optional<Integer> version) {
    return taskTemplateService.getAsTekton(name, version, Optional.of(team));
  }
  
  @GetMapping(value = "/query")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Search for Task Templates. If teams are provided it will query the teams. If no teams are provided it will query Global Task Templates")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskTemplateResponsePage queryTaskTemplates(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses",
      description = "List of statuses to filter for.", example = "active,inactive",
      required = false) @RequestParam(required = false, defaultValue = "active")  Optional<List<String>> statuses,
      @Parameter(name = "names",
      description = "List of TaskTemplate Names  to filter for. Defaults to all.", example = "switch,event-wait",
      required = false) @RequestParam(required = false)  Optional<List<String>> names,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "sort", description = "Ascending (ASC) or Descending (DESC) sort on creationDate", example = "ASC",
  required = true) @RequestParam(defaultValue = "ASC") Optional<Direction> sort) {
    return taskTemplateService.query(limit, page, sort, labels, statuses, names, Optional.of(team));
  }

  @PostMapping(value = "")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Create a new Task Template",
            description = "The name needs to be unique and must only contain alphanumeric and - characeters.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskTemplate createTaskTemplate(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @RequestBody TaskTemplate taskTemplate) {
    return taskTemplateService.create(taskTemplate, Optional.of(team));
  }

  @PostMapping(value = "", consumes = "application/x-yaml", produces = "application/x-yaml")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Create a new Task Template using Tekton Task YAML",
            description = "The name needs to be unique and must only contain alphanumeric and - characeters.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TektonTask createTaskTemplateYAML(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @RequestBody TektonTask taskTemplate) {
    return taskTemplateService.createAsTekton(taskTemplate, Optional.of(team));
  }

  @PutMapping(value = "")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Update, replace, or create new, Task Template",
            description = "The name must only contain alphanumeric and - characeters. If the name exists, apply will create a new version.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TaskTemplate applyTaskTemplate(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @RequestBody TaskTemplate taskTemplate,
      @Parameter(name = "replace",
      description = "Replace existing version",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean replace) {
    return taskTemplateService.apply(taskTemplate, replace, Optional.of(team));
  }

  @PutMapping(value = "", consumes = "application/x-yaml", produces = "application/x-yaml")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Update, replace, or create new using Tekton Task YAML",
            description = "The name must only contain alphanumeric and - characeters. If the name exists, apply will create a new version.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public TektonTask applyTaskTemplateYAML(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @RequestBody TektonTask taskTemplate,
      @Parameter(name = "replace",
      description = "Replace existing version",
      required = false) @RequestParam(required = false, defaultValue = "false") boolean replace) {
    return taskTemplateService.applyAsTekton(taskTemplate, replace, Optional.of(team));
  }
  
  @GetMapping(value = "/{name}/changelog")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  @Operation(summary = "Retrieve the changlog", description = "Retrieves each versions changelog and returns them all as a list.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<ChangeLogVersion> getChangelog(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "name",
      description = "Name of Task Template",
      required = true) @PathVariable String name) {
    return taskTemplateService.changelog(team + "/" + name);
  }

  @PostMapping(value = "/validate", consumes = "application/x-yaml", produces = "application/x-yaml")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TASKTEMPLATE, types = {AuthType.team})
  public void validateYaml(@RequestBody TektonTask tektonTask) {
    taskTemplateService.validateAsTekton(tektonTask);
  }
}
