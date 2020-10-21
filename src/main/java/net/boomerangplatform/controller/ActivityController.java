package net.boomerangplatform.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.InsightsSummary;
import net.boomerangplatform.model.ListActivityResponse;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.service.FlowTeamService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.service.crud.FlowActivityService;

@RestController
@RequestMapping("/workflow/")
public class ActivityController {

  @Autowired
  private FlowActivityService flowActivityService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private FlowWorkflowService workflowService;

  @Autowired
  private FlowTeamService flowTeamService;

  private static final String CREATIONDATESORT = "creationDate";

  @GetMapping(value = "/activity")
  public ListActivityResponse getFlowActivities(
      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @RequestParam Optional<String> sort, @RequestParam Optional<List<String>> workflowIds,
      @RequestParam Optional<List<String>> teamIds, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size, @RequestParam Optional<Long> fromDate,
      @RequestParam Optional<Long> toDate, @RequestParam Optional<List<String>> statuses,

      @RequestParam Optional<List<String>> triggers) {

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
    final FlowUserEntity user = userIdentityService.getCurrentUser();

    if (user == null) {
      return null;
    } else {
      return flowActivityService.getAllActivites(from, to, pageable, workflowIds, teamIds, statuses,
          triggers,sort.get(), order.get());
    }
  }

  @GetMapping(value = "/activity/{activityId}")
  public FlowActivity getFlowActivity(@PathVariable String activityId) {
    final ActivityEntity activity =
        flowActivityService.findWorkflowActivity(activityId);

    String workFlowId = activity.getWorkflowId();
    String teamId;
    String teamName;
    final FlowActivity response = new FlowActivity(activity);

    if ((workflowService.getWorkflow(workFlowId) != null)) {
      teamId = workflowService.getWorkflow(workFlowId).getFlowTeamId();
    } else {
      teamId = null;
    }

    if (teamId != null) {
      FlowTeamEntity team = flowTeamService.findById(teamId);
      teamName = team.getName();
      response.setTeamName(teamName);
    }

    final List<TaskExecutionEntity> steps = flowActivityService.getTaskExecutions(activityId);

    response.setSteps(steps);

    return response;
  }

  @GetMapping(value = "/activity/summary")
  public Map<String, Long> getFlowActivitySummary(
      @RequestParam(defaultValue = "ASC") Direction order,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) List<String> teamIds,
      @RequestParam(required = false) List<String> triggers,
      @RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate) {

    return flowActivityService.getActivitySummary(getPageable(page, size, sort), teamIds, triggers,
        fromDate, toDate);
  }

  @GetMapping(value = "/insights")
  public InsightsSummary getInsightsSummary(
      @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @RequestParam Optional<String> sort, @RequestParam Optional<String> teamId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size, @RequestParam Optional<Long> fromDate,
      @RequestParam Optional<Long> toDate) {

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

    return flowActivityService.getInsightsSummary(from, to, pageable, teamId);

  }

  @GetMapping(value = "/activity/{activityId}/log/{taskId}")
  @ResponseBody
  public ResponseEntity<StreamingResponseBody> getTaskLog(HttpServletResponse response,
      @PathVariable String activityId, @PathVariable String taskId) {
    response.setContentType("text/plain");
    response.setCharacterEncoding("UTF-8");
    return new ResponseEntity<>(flowActivityService.getTaskLog(activityId, taskId), HttpStatus.OK);
  }

  private Pageable getPageable(int page, int size, String sort) {
    Sort pagingSort = Sort.by(new Order(Direction.DESC, CREATIONDATESORT));
    if (StringUtils.isNotBlank(sort)) {
      Direction direction = Direction.ASC;
      pagingSort = Sort.by(new Order(direction, sort));
    }
    return PageRequest.of(page, size, pagingSort);
  }

}
