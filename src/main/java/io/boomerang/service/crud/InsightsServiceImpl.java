package io.boomerang.service.crud;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.boomerang.model.Execution;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.InsightsSummary;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.service.FlowTeamService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.service.FilterService;

@Service
public class InsightsServiceImpl implements InsightsService {

  private static final Logger LOGGER = LogManager.getLogger();
  
  @Autowired
  private FlowWorkflowActivityService activitiesService;

  @Autowired
  private FilterService filterService;

  @Autowired
  private FlowWorkflowService workflowService;

  @Autowired
  private FlowTeamService flowTeamService;

  @Override
  public InsightsSummary getInsights(Optional<Date> from, Optional<Date> to,
      Pageable pageable, Optional<List<String>> workflowIds, Optional<List<String>> teamIds, Optional<List<String>> scopes,
      Optional<List<String>> statuses, Optional<List<String>> triggers) {
    
    final List<String> workflowIdsList = filterService.getFilteredWorkflowIds(workflowIds, teamIds, scopes);

    LOGGER.debug("--> Workflow IDs: " + workflowIdsList.toString());
    
    final Page<ActivityEntity> records = activitiesService.getAllActivities(from, to, pageable,
        Optional.of(workflowIdsList), statuses, triggers);
    
    LOGGER.debug("--> Number of Workflow Records: " + records.getSize());
    
    final InsightsSummary response = new InsightsSummary();
    final List<FlowActivity> activities = filterService.convertActivityEntityToFlowActivity(records.getContent());
    List<Execution> executions = new ArrayList<>();
    Long totalExecutionTime = 0L;
    Long executionTime;

    for (FlowActivity activity : activities) {
      executionTime = activity.getDuration();
      if (executionTime != null) {
        totalExecutionTime = totalExecutionTime + executionTime;
      }
      addActivityDetail(executions, activity);
    }
    response.setTotalActivitiesExecuted(executions.size());
    response.setExecutions(executions);

    if (response.getTotalActivitiesExecuted() != 0) {
      response.setMedianExecutionTime(totalExecutionTime / executions.size());
    } else {
      response.setMedianExecutionTime(0L);
    }
    return response;
  }
  
  //TODO: determine if this refactor and adjusting how teamId is used works or breaks things.
  private void addActivityDetail(List<Execution> executions, FlowActivity activity) {
    String teamName = null;
    String workflowName = null;
    String workflowId = activity.getWorkflowId();
    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
    if (workflow != null) {
      workflowName = workflow.getName();
    }

    if (WorkflowScope.team.equals(activity.getScope())) {
      TeamEntity team = flowTeamService.findById(activity.getTeamId());
      if (team != null) {
        teamName = team.getName();
      }
    }
    Execution execution = createExecution(activity, teamName, workflowName, workflowId);
    executions.add(execution);
  }
  
  private Execution createExecution(FlowActivity activity, String teamName, String workflowName,
      String workflowId) {
    Execution execution = new Execution();
    execution.setActivityId(activity.getId());
    execution.setStatus(activity.getStatus());
    execution.setDuration(activity.getDuration());
    execution.setCreationDate(activity.getCreationDate());
    execution.setTeamName(teamName);
    execution.setWorkflowName(workflowName);
    execution.setWorkflowId(workflowId);
    return execution;
  }
}
