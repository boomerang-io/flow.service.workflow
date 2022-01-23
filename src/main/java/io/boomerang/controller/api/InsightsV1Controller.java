package io.boomerang.controller.api;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.InsightsSummary;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.service.crud.InsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1")
@Tag(name = "Insights Management",
    description = "Provide the ability to search and retrieve workflow insights.")
public class InsightsV1Controller {

  @Autowired
  private InsightsService insightsService;

  @GetMapping(value = "/insights")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Retrieve insights for a team.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public InsightsSummary getTeamInsights(
      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @RequestParam Optional<List<String>> scopes, 
      @RequestParam Optional<String> sort,
      @RequestParam Optional<List<String>> workflowIds,
      @RequestParam Optional<List<String>> teamIds, 
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size, 
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
    Sort pagingSort = Sort.by(new Order(Direction.DESC, "creationDate"));
    final Pageable pageable = PageRequest.of(0, 10, pagingSort);

    return insightsService.getInsights(from, to, pageable, workflowIds, teamIds, scopes, statuses, triggers);
  }
  
//  TODO: future user activity summary
  

//  public ListActivityResponse getFlowActivities(
//      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
//      @RequestParam Optional<List<String>> scopes, 
//      @RequestParam Optional<String> sort,
//      @RequestParam Optional<List<String>> workflowIds,
//      @RequestParam Optional<List<String>> teamIds, 
//      @RequestParam(defaultValue = "0") int page,
//      @RequestParam(defaultValue = "2147483647") int size, 
//      @RequestParam Optional<Long> fromDate,
//      @RequestParam Optional<Long> toDate, 
//      @RequestParam Optional<List<String>> statuses,
//      @RequestParam Optional<List<String>> triggers) {
//
//    Optional<Date> from = Optional.empty();
//    Optional<Date> to = Optional.empty();
//    if (fromDate.isPresent()) {
//      from = Optional.of(new Date(fromDate.get() * 1000));
//    }
//    if (toDate.isPresent()) {
//      to = Optional.of(new Date(toDate.get() * 1000));
//    }
//
//    Sort pagingSort = Sort.by(new Order(Direction.DESC, CREATIONDATESORT));
//    if (sort.isPresent()) {
//      Direction direction = Direction.ASC;
//      final String sortByKey = sort.get();
//      if (order.isPresent()) {
//        direction = order.get();
//
//      }
//      pagingSort = Sort.by(new Order(direction, sortByKey));
//    }
//    final Pageable pageable = PageRequest.of(page, size, pagingSort);
//    return flowActivityService.getAllActivites(from, to, pageable, workflowIds, teamIds, statuses,
//        triggers, scopes, sort.get(), order.get());
}
