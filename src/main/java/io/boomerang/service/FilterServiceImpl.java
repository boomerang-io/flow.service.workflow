package io.boomerang.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.FlowActivity;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.service.crud.TeamService;

@Service
public class FilterServiceImpl implements FilterService {

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private TeamService teamService;

  @Autowired
  private FlowWorkflowService flowWorkflowService;
  
  /*
   * Converts from ActivityEntity DB model to consumable FlowActivity
   * 
   * @param list of ActivityEntity's
   * @return list of FlowActivity
   */
  @Override
  public List<FlowActivity> convertActivityEntityToFlowActivity(List<ActivityEntity> records) {

    final List<FlowActivity> flowActivities = new LinkedList<>();

    for (final ActivityEntity record : records) {
      final FlowActivity flow = new FlowActivity(record);
      final WorkflowEntity workflow = flowWorkflowService.getWorkflow(record.getWorkflowId());

      if (workflow != null) {
        flow.setWorkflowName(workflow.getName());
        flow.setDescription(workflow.getDescription());
        flow.setIcon(workflow.getIcon());
        flow.setShortDescription(workflow.getShortDescription());
      }

      flowActivities.add(flow);
    }
    return flowActivities;
  }
  
  /*
   * Generates the workflowIds based on optional lists of workflowIds, scopes, and teamIds
   * 
   * @param list of WorkflowIds
   * @param list of Scopes
   * @param list of TeamIds
   * 
   * @return list of filtered WorkflowIds
   */
  @Override
  public List<String> getFilteredWorkflowIds(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes) {
    final FlowUserEntity user = userIdentityService.getCurrentUser();
    List<String> workflowIdsList = new LinkedList<>();

    if (!workflowIds.isPresent()) {
      if (scopes.isPresent() && !scopes.get().isEmpty()) {
        List<String> scopeList = scopes.get();
        if (scopeList.contains("user")) {
          addUserWorkflows(user, workflowIdsList);
        }
        if (scopeList.contains("system") && user.getType() == UserType.admin) {
          addSystemWorkflows(workflowIdsList);
        }
        if (scopeList.contains("team")) {
          addTeamWorkflows(user, workflowIdsList, teamIds);
        }
      } else if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
        addTeamWorkflows(user, workflowIdsList, teamIds);
      } else {
        addUserWorkflows(user, workflowIdsList);
        addTeamWorkflows(user, workflowIdsList, teamIds);
        if (user.getType() == UserType.admin) {
          addSystemWorkflows(workflowIdsList);
        }
      }
    } else {
      List<String> requestWorkflowList = workflowIds.get();
      workflowIdsList.addAll(requestWorkflowList);
    }
    return workflowIdsList;
  }

  private void addTeamWorkflows(final FlowUserEntity user, List<String> workflowIdsList,
      Optional<List<String>> teamIds) {

    if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
      List<WorkflowEntity> allTeamWorkflows =
          this.flowWorkflowService.getWorkflowsForTeams(teamIds.get());
      List<String> allTeamWorkflowsIds =
          allTeamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
      workflowIdsList.addAll(allTeamWorkflowsIds);
    } else {
      if (user.getType() == UserType.admin) {
        List<WorkflowEntity> allTeamWorkflows = this.flowWorkflowService.getTeamWorkflows();
        List<String> workflowIds =
            allTeamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
        workflowIdsList.addAll(workflowIds);
      } else {
        List<TeamEntity> flowTeam = teamService.getUsersTeamListing(user);
        List<String> flowTeamIds =
            flowTeam.stream().map(TeamEntity::getId).collect(Collectors.toList());
        List<WorkflowEntity> teamWorkflows =
            this.flowWorkflowService.getWorkflowsForTeams(flowTeamIds);
        List<String> allTeamWorkflowsIds =
            teamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
        workflowIdsList.addAll(allTeamWorkflowsIds);
      }
    }
  }

  private void addSystemWorkflows(List<String> workflowIdsList) {
    List<WorkflowEntity> systemWorkflows = this.flowWorkflowService.getSystemWorkflows();
    List<String> systemWorkflowsIds =
        systemWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(systemWorkflowsIds);
  }

  private void addUserWorkflows(final FlowUserEntity user, List<String> workflowIdsList) {
    String userId = user.getId();
    List<WorkflowEntity> userWorkflows = this.flowWorkflowService.getUserWorkflows(userId);
    List<String> userWorkflowIds =
        userWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(userWorkflowIds);
  }
}
