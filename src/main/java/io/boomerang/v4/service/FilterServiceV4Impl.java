package io.boomerang.v4.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.UserType;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.crud.TeamService;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.data.repository.RelationshipRepository;

@Service
public class FilterServiceV4Impl implements FilterServiceV4 {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private UserIdentityService userIdentityService;
  
  @Autowired
  private RelationshipRepository relationshipRepository;

  @Autowired
  private TeamService teamService;

//  @Autowired
//  private FlowWorkflowService flowWorkflowService;
//  
//  /*
//   * Converts from ActivityEntity DB model to consumable FlowActivity
//   * 
//   * @param list of ActivityEntity's
//   * @return list of FlowActivity
//   */
//  @Override
//  public List<FlowActivity> convertActivityEntityToFlowActivity(List<ActivityEntity> records) {
//
//    final List<FlowActivity> flowActivities = new LinkedList<>();
//
//    for (final ActivityEntity record : records) {
//      final FlowActivity flow = new FlowActivity(record);
//      final WorkflowEntity workflow = flowWorkflowService.getWorkflow(record.getWorkflowId());
//
//      if (workflow != null) {
//        flow.setWorkflowName(workflow.getName());
//        flow.setDescription(workflow.getDescription());
//        flow.setIcon(workflow.getIcon());
//        flow.setShortDescription(workflow.getShortDescription());
//      }
//
//      flowActivities.add(flow);
//    }
//    return flowActivities;
//  }
  
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
    FlowUserEntity user = null;
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
    FlowUserEntity user = userIdentityService.getUserByEmail(userEmail);
    Boolean isAdmin = false;
    if (user!= null && user.getType() == UserType.admin) {
      isAdmin = true;
    }
    return getFilteredWorkflowIdsList(workflowIds, teamIds, scopes, user, isAdmin);
  }
    
    private List<String> getFilteredWorkflowIdsList(Optional<List<String>> workflowIds,
        Optional<List<String>> teamIds, Optional<List<String>> scopes, FlowUserEntity user,
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

  private void addTeamWorkflows(Boolean isAdmin, final FlowUserEntity user, List<String> workflowIdsList,
      Optional<List<String>> teamIds) {
    List<RelationshipEntity> relationships = null;
    if (teamIds.isPresent() && !teamIds.get().isEmpty()) {
      relationships = this.relationshipRepository.findByFromTypeAndToTypeAndToRefIn("WorkflowRun","Team",teamIds.get());
    } else {
      if (isAdmin) {
        relationships = this.relationshipRepository.findByFromTypeAndToType("WorkflowRun","Team");
      } else if (user != null) {
        List<TeamEntity> flowTeam = teamService.getUsersTeamListing(user);
        List<String> flowTeamIds =
            flowTeam.stream().map(TeamEntity::getId).collect(Collectors.toList());
        relationships = this.relationshipRepository.findByFromTypeAndToTypeAndToRefIn("WorkflowRun","Team",flowTeamIds);
      }
    }
    if (relationships != null) {
    List<String> teamWorkflowsIds =
        relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
    workflowIdsList.addAll(teamWorkflowsIds);
    }
  }

  private void addSystemWorkflows(List<String> workflowIdsList) {
    List<RelationshipEntity> relationships = this.relationshipRepository.findByFromTypeAndToType("WorkflowRun","System");
    List<String> systemWorkflowsIds =
        relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
    workflowIdsList.addAll(systemWorkflowsIds);
  }

  private void addUserWorkflows(final FlowUserEntity user, List<String> workflowIdsList) {
    String userId = user.getId();
    List<RelationshipEntity> relationships = this.relationshipRepository.findByFromTypeAndToTypeAndToRef("WorkflowRun","User",userId);
    List<String> userWorkflowIds =
        relationships.stream().map(RelationshipEntity::getFromRef).collect(Collectors.toList());
    workflowIdsList.addAll(userWorkflowIds);
  }
}
