package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.InsightsSummary;
import io.boomerang.model.ListActivityResponse;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.TeamWorkflowSummary;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowTeamEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.TeamService;

@RestController
@RequestMapping("/workflow/")
public class ActivityController {

  @Autowired
  private FlowActivityService flowActivityService;

  @Autowired
  private FlowWorkflowService workflowService;

  @Autowired
  private TeamService teamService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private RevisionService revisionService;

  private static final String CREATIONDATESORT = "creationDate";


  @GetMapping(value = "/activity")
  public ListActivityResponse getFlowActivities(
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
    return flowActivityService.getAllActivites(from, to, pageable, workflowIds, teamIds, statuses,
        triggers, scopes, sort.get(), order.get());
  }

  @DeleteMapping(value = "/activity/{activityId}/cancel")
  public ResponseEntity<FlowActivity> cancelFlowActivity(@PathVariable String activityId) {
    ActivityEntity activity = flowActivityService.findWorkflowActivity(activityId);
    if (activity == null) {
      return null;
    }

    flowActivityService.cancelWorkflowActivity(activity.getId(), null);

    activity = flowActivityService.findWorkflowActivity(activityId);
    final List<TaskExecutionResponse> steps = flowActivityService.getTaskExecutions(activityId);
    final FlowActivity response = new FlowActivity(activity);
    response.setSteps(steps);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }


  @GetMapping(value = "/activity/{activityId}")
  public ResponseEntity<FlowActivity> getFlowActivity(@PathVariable String activityId) {
    final ActivityEntity activity = flowActivityService.findWorkflowActivity(activityId);

    String workFlowId = activity.getWorkflowId();
    String teamId;
    String teamName;
    final FlowActivity response = new FlowActivity(activity);

    WorkflowEntity entity = this.workflowService.getWorkflow(workFlowId);
    WorkflowScope scope = entity.getScope();
    if (scope == null) {
      scope = WorkflowScope.team;
    }

    response.setScope(scope);

    if (activity.getWorkflowRevisionid() != null) {
      Optional<RevisionEntity> revisionOpt =
          this.revisionService.getRevision(activity.getWorkflowRevisionid());
      if (revisionOpt.isPresent()) {
        RevisionEntity revision = revisionOpt.get();
        response.setWorkflowRevisionVersion(revision.getVersion());
      }
    }

    if ((workflowService.getWorkflow(workFlowId) != null)) {
      teamId = workflowService.getWorkflow(workFlowId).getFlowTeamId();
    } else {
      teamId = null;
    }

    if (teamId != null) {

      final FlowUserEntity user = userIdentityService.getCurrentUser();

      if (user != null) {
        List<String> teamIdList =
            user.getType().equals(UserType.admin) || user.getType().equals(UserType.operator)
                ? teamService.getAllTeams().stream().map(TeamWorkflowSummary::getId)
                    .collect(Collectors.toList())
                : teamService.getUserTeams(user).stream().map(TeamWorkflowSummary::getId)
                    .collect(Collectors.toList());
        if (!teamIdList.contains(teamId)) {
          return new ResponseEntity<>(new FlowActivity(), HttpStatus.FORBIDDEN);
        }
      }

      FlowTeamEntity team = teamService.getTeamById(teamId);
      if (team != null) {
        teamName = team.getName();
        response.setTeamName(teamName);
      }
    }

    final List<TaskExecutionResponse> steps = flowActivityService.getTaskExecutions(activityId);

    response.setSteps(steps);

    return new ResponseEntity<>(response, HttpStatus.OK);
  }

  @GetMapping(value = "/activity/summary")
  public Map<String, Long> getFlowActivitySummary(
      @RequestParam(defaultValue = "ASC") Direction order,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "2147483647") int size,
      @RequestParam(required = false) String sort, @RequestParam Optional<List<String>> teamIds,
      @RequestParam(required = false) List<String> triggers,
      @RequestParam Optional<List<String>> scopes, @RequestParam Optional<List<String>> workflowIds,
      @RequestParam(required = false) Long fromDate, @RequestParam(required = false) Long toDate) {

    return flowActivityService.getActivitySummary(getPageable(page, size, sort), teamIds, triggers,
        workflowIds, scopes, fromDate, toDate);
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
