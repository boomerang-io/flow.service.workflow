package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.ref.WorkflowRunInsight;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.service.InsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/team/{team}")
@Tag(name = "Insights Management",
    description = "Provide the ability to search and retrieve Insights.")
public class TeamInsightsV2Controller {

  @Autowired
  private InsightsService insightsService;

  private static final String CREATIONDATESORT = "creationDate";

  @GetMapping(value = "/insights")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.INSIGHTS, types = {AuthType.team})
  @Operation(summary = "Retrieve insights for a team.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowRunInsight getTeamInsights(
      @Parameter(name = "team",
      description = "Owning team name.",
      example = "my-amazing-team",
      required = true) @PathVariable String team,
      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @RequestParam Optional<String> sort,
      @RequestParam Optional<List<String>> workflows,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size, 
      @RequestParam Optional<Long> fromDate,
      @RequestParam Optional<Long> toDate, 
      @RequestParam Optional<List<String>> statuses,
      @RequestParam Optional<List<String>> triggers) {

    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get()));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get()));
    }
    Sort pagingSort = Sort.by(new Order(Direction.DESC, CREATIONDATESORT));
    if (sort.isPresent()) {
      Direction direction = Direction.ASC;
      final String sortByKey = sort.get();
      if (order.isPresent()) {
        direction = order.get();

      }
      pagingSort = Sort.by(new Order(direction, sortByKey));
    }
    final Pageable pageable = PageRequest.of(page, size, pagingSort);

    return insightsService.getInsights(team, from, to, pageable, workflows, statuses, triggers);
  }
}
