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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.InsightsSummary;
import io.boomerang.service.crud.InsightsService;

@RestController
@RequestMapping("/workflow/")
public class InsightsController {

  @Autowired
  private InsightsService insightsService;

  private static final String CREATIONDATESORT = "creationDate";
  
  @GetMapping(value = "/insights")
  public InsightsSummary getInsights(
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
    
    return insightsService.getInsights(from, to, pageable, workflowIds, teamIds, scopes, statuses, triggers);
  }
}
