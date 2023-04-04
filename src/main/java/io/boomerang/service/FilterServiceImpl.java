package io.boomerang.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.FlowActivity;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.service.TeamService;

@Service
public class FilterServiceImpl implements FilterService {

  private static final Logger LOGGER = LogManager.getLogger();

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
    UserEntity user = null;
    Boolean isAdmin = false;
    
    LOGGER.info("Current User Scope: " + userIdentityService.getCurrentScope());
    switch(userIdentityService.getCurrentScope()) {
      case user:
        user = userIdentityService.getCurrentUser();
        if (user.getType() == UserType.admin) {
          isAdmin = true;
        }
        break;
      case team:
        
        break;
      case global:
        isAdmin = true;
        break;
    }

    return getFilteredWorkflowIdsList(workflowIds, teamIds, scopes, user, isAdmin);
  }

  /*
   * Generates the workflowIds based on optional lists of workflowIds, scopes, and teamIds
   * 
   * @param list of WorkflowIds
   * @param list of Scopes
   * @param list of TeamIds
   * @param FlowUserEntity user
   * 
   * @return list of filtered WorkflowIds
   */
  @Override
  public List<String> getFilteredWorkflowIdsForUserEmail(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes, String userEmail) {
    UserEntity user = userIdentityService.getUserByEmail(userEmail);
    Boolean isAdmin = false;
    if (user!= null && user.getType() == UserType.admin) {
      isAdmin = true;
    }
    return getFilteredWorkflowIdsList(workflowIds, teamIds, scopes, user, isAdmin);
  }
    
    private List<String> getFilteredWorkflowIdsList(Optional<List<String>> workflowIds,
        Optional<List<String>> teamIds, Optional<List<String>> scopes, UserEntity user,
        Boolean isAdmin) {
    List<String> workflowIdsList = new LinkedList<>();
    if (!workflowIds.isPresent()) {
      if (scopes.isPresent() && !scopes.get().isEmpty()) {
        List<String> scopeList = scopes.get();
        if (scopeList.contains("user") && user != null) {
          addUserWorkflows(user, workflowIdsList);
        }
        if (scopeList.contains("system") && isAdmin) {
          addSystemWorkflows(workflowIdsList);
        }
        if (scopeList.contains("team")) {
          addTeamWorkflows(isAdmin, user, workflowIdsList, teamIds);
        }
      } else if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
        addTeamWorkflows(isAdmin, user, workflowIdsList, teamIds);
      } else {
        if (user != null) {
          addUserWorkflows(user, workflowIdsList);
        }
        addTeamWorkflows(isAdmin, user, workflowIdsList, teamIds);
        if (isAdmin) {
          addSystemWorkflows(workflowIdsList);
        }
      }
    } else {
      List<String> requestWorkflowList = workflowIds.get();
      workflowIdsList.addAll(requestWorkflowList);
    }
    return workflowIdsList;
  }

  private void addTeamWorkflows(Boolean isAdmin, final UserEntity user, List<String> workflowIdsList,
      Optional<List<String>> teamIds) {
    List<WorkflowEntity> teamWorkflows = null;
    if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
      teamWorkflows = this.flowWorkflowService.getWorkflowsForTeams(teamIds.get());
    } else {
      if (isAdmin) {
        teamWorkflows = this.flowWorkflowService.getTeamWorkflows();
      } else if (user != null) {
        List<TeamEntity> flowTeam = teamService.getUsersTeamListing(user);
        List<String> flowTeamIds =
            flowTeam.stream().map(TeamEntity::getId).collect(Collectors.toList());
        teamWorkflows = this.flowWorkflowService.getWorkflowsForTeams(flowTeamIds);
      }
    }
    List<String> teamWorkflowsIds =
        teamWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(teamWorkflowsIds);
  }

  private void addSystemWorkflows(List<String> workflowIdsList) {
    List<WorkflowEntity> systemWorkflows = this.flowWorkflowService.getSystemWorkflows();
    List<String> systemWorkflowsIds =
        systemWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(systemWorkflowsIds);
  }

  private void addUserWorkflows(final UserEntity user, List<String> workflowIdsList) {
    String userId = user.getId();
    List<WorkflowEntity> userWorkflows = this.flowWorkflowService.getWorkflowsForUser(userId);
    List<String> userWorkflowIds =
        userWorkflows.stream().map(WorkflowEntity::getId).collect(Collectors.toList());
    workflowIdsList.addAll(userWorkflowIds);
  }
}
