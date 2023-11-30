package io.boomerang.security.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import io.boomerang.client.model.Team;
import io.boomerang.model.FlowTeam;
import io.boomerang.model.TemplateScope;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.security.service.UserValidationService;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.crud.TeamService;

@Service
public class UserValidationServiceImpl implements UserValidationService {

  @Autowired
  private TeamService teamService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private FlowWorkflowService workflowRepository;

  @Override
  public void validateUserForTeam(String teamId) {
    FlowUserEntity user = userIdentityService.getCurrentUser();
    FlowTeam team = teamService.getTeamByIdDetailed(teamId);
    List<String> userIds = new ArrayList<>();
    if (team.getUsers() != null) {
      for (FlowUserEntity teamUser : team.getUsers()) {
        userIds.add(teamUser.getId());
      }
    }
    List<String> userTeamIds = new ArrayList<>();
    if (user.getTeams() != null) {
      for (Team userTeam : user.getTeams()) {
        userTeamIds.add(userTeam.getId());
      }
    }
    if (user.getType() != UserType.admin && user.getType() != UserType.operator
        && !userIds.contains(user.getId()) && !userTeamIds.contains(team.getHigherLevelGroupId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
  
  @Override
  public void validateUserAccessForWorkflow(WorkflowScope scope, String flowTeamId, String flowOwnerUserId, boolean editable) {
    FlowUserEntity user = userIdentityService.getCurrentUser();
    if(scope == WorkflowScope.user) {
      this.validateAccessForUserScope(user, flowOwnerUserId);
    } else if(scope == WorkflowScope.team) {
      this.validateAccessForTeamScope(user, flowTeamId, editable);	
    } else {
      this.validateAccessForSystemScope(user);	
    }
  }
  
  @Override
  public void validateUserAccessForTaskTemplate(TemplateScope scope, String taskTeamId, boolean editable) {
	FlowUserEntity user = userIdentityService.getCurrentUser();
    if(scope == TemplateScope.team) {
      this.validateAccessForTeamScope(user, taskTeamId, editable);	
    } else {
      this.validateAccessForSystemScope(user);	
    }
  }
  
  @Override
  public void validateUserForWorkflow(String workflowId) {
    FlowUserEntity user = userIdentityService.getCurrentUser();
    WorkflowEntity workflow = workflowRepository.getWorkflow(workflowId);
    WorkflowScope scope = workflow.getScope();
    if (workflow.getScope() == WorkflowScope.team) {

      FlowTeam team = teamService.getTeamByIdDetailed(workflow.getFlowTeamId());

      List<String> userIds = new ArrayList<>();
      if (team.getUsers() != null) {
        for (FlowUserEntity teamUser : team.getUsers()) {
          userIds.add(teamUser.getId());
        }
      }

      List<String> userTeamIds = new ArrayList<>();
      if (user.getTeams() != null) {
        for (Team userTeam : user.getTeams()) {
          userTeamIds.add(userTeam.getId());
        }
      }
      if (user.getType() != UserType.admin && user.getType() != UserType.operator
          && !userIds.contains(user.getId())
          && !userTeamIds.contains(team.getHigherLevelGroupId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      }
    }

    if (scope == WorkflowScope.user && !user.getId().equals(workflow.getOwnerUserId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    if (scope == WorkflowScope.system && user.getType() != UserType.admin
        && user.getType() != UserType.operator) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public void validateUserAdminOrOperator() {
    FlowUserEntity user = userIdentityService.getCurrentUser();
    if (!isPlatformAdminOrOperator(user)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
  
  private void validateAccessForUserScope(FlowUserEntity user, String ownerUserId) {
	if (isPlatformAdminOrOperator(user)) {
	  // Platform Administrator and Operator always has full access.
	  return;	
	}
	
    if(!Strings.isBlank(ownerUserId) && !user.getId().equals(ownerUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
  
  private void validateAccessForTeamScope(FlowUserEntity user, String teamId, boolean editable) {
    if (isPlatformAdminOrOperator(user)) {
	  // Platform Administrator and Operator always has full access.
	  return;	
	}
	// validate team level roles when team id is provided
	List<TeamEntity> userTeams = teamService.getUsersTeamListing(user);   
	TeamEntity matchedTeam = userTeams.stream().filter(t -> teamId.equals(t.getId())).findFirst().orElse(null);
	if(matchedTeam == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);    	  
	}
	this.validateTeamLevelRoles(matchedTeam, editable);
  }
  
  private void validateAccessForSystemScope(FlowUserEntity user) {
    if (!isPlatformAdminOrOperator(user)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  private void validateTeamLevelRoles(TeamEntity team, boolean editable) {
    if(team.getUserRoles() == null || team.getUserRoles().isEmpty()) {
      // skip if team user roles is not enabled
      return;
    }
    
    if(editable && !team.getUserRoles().contains("operator")) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
  
  private boolean isPlatformAdminOrOperator(FlowUserEntity user) {
	  return user.getType() == UserType.admin || user.getType() == UserType.operator;
  }

}
