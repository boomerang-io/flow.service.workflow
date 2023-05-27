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
import io.boomerang.service.ActionService;
import io.boomerang.v4.model.Action;
import io.boomerang.v4.model.ActionRequest;
import io.boomerang.v4.model.ActionSummary;
import io.boomerang.v4.model.enums.ref.ActionStatus;
import io.boomerang.v4.model.enums.ref.ActionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/action")
@Tag(name = "Actions Management",
description = "Create and manage Manual and Approval Actions.")
public class ActionV2Controller {

  @Autowired
  private ActionService actionService;

  @PutMapping(value = "/")
  @Operation(summary = "Provide an update for an Action")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void action(@RequestBody List<ActionRequest> request) {
      actionService.action(request);
  }

  @GetMapping(value = "/{actionId}")
  @Operation(summary = "Retrieve a specific Action by Id")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void get(
      @Parameter(name = "actionId", description = "ID of Action",
      required = true) @PathVariable String actionId) {
      actionService.get(actionId);
  }

  @GetMapping(value = "/")
  @Operation(summary = "Retrieve a specifc Action by TaskRun")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void getByTaskRun(
      @Parameter(name = "taskRunId", description = "Retrieve Action by TaskRun",
      required = true) @RequestParam(required = true) String taskRunId) {
      actionService.getByTaskRun(taskRunId);
  }

  @GetMapping(value = "/query")
  @Operation(summary = "Search for Actions")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<Action> query(
      @Parameter(name = "types",
      description = "List of types to filter for. Defaults to all.", example = "manual,approval",
      required = false) @RequestParam(required = false)  Optional<List<ActionType>> types,
      @Parameter(name = "statuses",
      description = "List of statuses to filter for. Defaults to all.", example = "approved,rejected,submitted",
      required = false) @RequestParam(required = false)  Optional<List<ActionStatus>> statuses,
      @Parameter(name = "workflows", description = "List of workflows to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "teams", description = "List of teams to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
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
    return actionService.query(from, to, pageable, types, statuses, workflows, teams);
  }
  
  @GetMapping(value = "/summary")
  @Operation(summary = "Get Actions Summary")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ActionSummary summary(
      @Parameter(name = "workflows", description = "List of workflows to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "teams", description = "List of teams to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
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
    return actionService.summary(from, to, workflows, teams);
  }
}
