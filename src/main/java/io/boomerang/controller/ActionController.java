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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.ActionSummary;
import io.boomerang.model.ApprovalRequest;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.model.ListActionResponse;
import io.boomerang.mongo.model.ManualType;
import io.boomerang.service.ActionService;
import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping("/workflow")
@Hidden
public class ActionController {

  private static final String CREATIONDATESORT = "creationDate";

  @Autowired
  private ActionService actionService;

  @PutMapping(value = "/actions/action")
  public void actionApproval(@RequestBody ApprovalRequest request) {
    actionService.actionApproval(request);
  }

  @GetMapping(value = "/actions/summary")
  public ActionSummary getActions(@RequestParam Long fromDate,
      @RequestParam Long toDate) {
    
    Date from = new Date(fromDate * 1000);
    Date to = new Date(toDate * 1000);
   
    return actionService.getActionSummary(from, to);
  }
      
  @GetMapping(value = "/actions")
  public ListActionResponse getActions(
      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @RequestParam Optional<List<String>> scopes, 
      @RequestParam Optional<String> sort,
      @RequestParam Optional<ApprovalStatus> status,
      @RequestParam Optional<List<String>> workflowIds,
      @RequestParam Optional<List<String>> teamIds, 
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size, 
      @RequestParam Optional<Long> fromDate,
      @RequestParam Optional<Long> toDate, 
      @RequestParam Optional<ManualType> type) {
    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get() * 1000));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get() * 1000));
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
    return actionService.getAllActions(from, to, pageable, workflowIds, teamIds, 
        type, scopes, CREATIONDATESORT, order.get(), status);
  }
}
