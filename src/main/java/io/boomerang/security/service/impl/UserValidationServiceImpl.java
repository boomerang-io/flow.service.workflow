package io.boomerang.security.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import io.boomerang.client.model.Team;
import io.boomerang.model.FlowTeam;
import io.boomerang.mongo.entity.FlowUserEntity;
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
    if (user.getType() != UserType.admin && user.getType() != UserType.operator) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public void validateUserById(String userId) {
    FlowUserEntity user = userIdentityService.getCurrentUser();
    if (user.getType() != UserType.admin && user.getType() != UserType.operator
        && !userId.equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

}
