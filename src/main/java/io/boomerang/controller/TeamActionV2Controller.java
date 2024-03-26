package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Action;
import io.boomerang.model.ActionRequest;
import io.boomerang.model.ActionSummary;
import io.boomerang.model.enums.ref.ActionStatus;
import io.boomerang.model.enums.ref.ActionType;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.service.ActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/team/{team}/action")
@Tag(name = "Actions Management",
description = "Create and manage Manual and Approval Actions.")
public class TeamActionV2Controller {

  @Autowired
  private ActionService actionService;

  @GetMapping(value = "/{actionId}")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.ACTION, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  @Operation(summary = "Retrieve a specific Action by Id")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Action get(
      @Parameter(name = "workflow", description = "Workflow reference",
      required = true) @PathVariable String workflow,
  @Parameter(name = "team",
  description = "Owning team name.",
  example = "my-amazing-team",
  required = true) @PathVariable String team,
      @Parameter(name = "actionId", description = "ID of Action",
      required = true) @PathVariable String actionId) {
      return actionService.get(team, actionId);
  }
  
//  @GetMapping(value = "")
//  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.ACTION, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
//  @Operation(summary = "Retrieve a specifc Action by TaskRun")
//  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
//      @ApiResponse(responseCode = "400", description = "Bad Request")})
//  public Action getByTaskRun(
//      @Parameter(name = "taskRunId", description = "Retrieve Action by TaskRun",
//      required = true) @RequestParam(required = true) String taskRunId) {
//      return actionService.getByTaskRun(taskRunId);
//  }

  @PutMapping(value = "")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.ACTION, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  @Operation(summary = "Provide an update for an Action")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void action(
      @Parameter(name = "workflow", description = "Workflow reference",
      required = true) @PathVariable String workflow,
  @Parameter(name = "team",
  description = "Owning team name.",
  example = "my-amazing-team",
  required = true) @PathVariable String team,
  @RequestBody List<ActionRequest> request) {
      actionService.action(team, request);
  }

  @GetMapping(value = "/query")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.ACTION, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  @Operation(summary = "Search for Actions")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<Action> query(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "types",
      description = "List of types to filter for. Defaults to all.", example = "manual,approval",
      required = false) @RequestParam(required = false)  Optional<List<ActionType>> types,
      @Parameter(name = "statuses",
      description = "List of statuses to filter for. Defaults to all.", example = "approved,rejected,submitted",
      required = false) @RequestParam(required = false)  Optional<List<ActionStatus>> statuses,
      @Parameter(name = "workflows", description = "List of workflows to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page,
      @Parameter(name = "order", description = "Ascending or Descending (default) order", example = "0",
      required = false) @RequestParam(defaultValue = "DESC") Optional<Direction> order,
      @Parameter(name = "sort", description = "The element to sort on", example = "0",
      required = false) @RequestParam(defaultValue = "creationDate") Optional<String> sort,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {
    final Sort pageingSort = Sort.by(new Order(order.get(), sort.get()));
    final Pageable pageable = PageRequest.of(page, limit, pageingSort);
    
    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get()));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get()));
    }
    return actionService.query(team, from, to, pageable, types, statuses, workflows);
  }
  
  @GetMapping(value = "/summary")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.ACTION, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  @Operation(summary = "Get Actions Summary")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ActionSummary summary(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @Parameter(name = "workflows", description = "List of workflows to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {    
    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get()));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get()));
    }
    return actionService.summary(team, from, to, workflows);
  }
}
