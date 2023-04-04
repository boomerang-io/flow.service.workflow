
package io.boomerang.v4.controller;

import java.util.ArrayList;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.model.ActionSummary;
import io.boomerang.model.ApprovalRequest;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.model.ListActionResponse;
import io.boomerang.mongo.model.ManualType;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.v4.service.ActionServiceV4;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


@RestController
@RequestMapping("/api/v2/action")
@Hidden
public class ActionV2Controller {

  private static final String CREATIONDATESORT = "creationDate";

  @Autowired
  private ActionServiceV4 actionService;

  @PutMapping(value = "/")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Provide an Approval for an Action")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void actionApproval(@RequestBody String request)
      throws JsonProcessingException {
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    Object requestObject = mapper.readValue(request, Object.class);
    if (requestObject instanceof ArrayList) {
      List<ApprovalRequest> approvalList =
          mapper.readValue(request, new TypeReference<List<ApprovalRequest>>() {});
      if (approvalList != null) {
        for (ApprovalRequest req : approvalList) {
          actionService.actionApproval(req);
        }
      }
    } else {
      ApprovalRequest req = mapper.readValue(request, ApprovalRequest.class);
      actionService.actionApproval(req);
    }
  }

  @GetMapping(value = "/actions/summary")
  public ActionSummary getActions(@RequestParam Optional<ApprovalStatus> status,
      @RequestParam Optional<List<String>> scopes, 
      @RequestParam Optional<List<String>> workflowIds,
      @RequestParam Optional<List<String>> teamIds, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size, @RequestParam Optional<Long> fromDate,
      @RequestParam Optional<Long> toDate, @RequestParam Optional<ManualType> type) {

    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get() * 1000));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get() * 1000));
    }

    return actionService.getActionSummary(from, to, workflowIds, teamIds, status,scopes);
  }

  @GetMapping(value = "/actions")
  public ListActionResponse getActions(
      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @RequestParam Optional<List<String>> scopes, @RequestParam Optional<String> sort,
      @RequestParam Optional<ApprovalStatus> status,
      @RequestParam Optional<List<String>> workflowIds,
      @RequestParam Optional<List<String>> teamIds, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size, @RequestParam Optional<Long> fromDate,
      @RequestParam Optional<Long> toDate, @RequestParam Optional<ManualType> type) {
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
    return actionService.getAllActions(from, to, pageable, workflowIds, teamIds, type, scopes,
        CREATIONDATESORT, order.get(), status);
  }
}
