package net.boomerangplatform.controller;

import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.CreateFlowTeam;
import net.boomerangplatform.model.TeamWorkflowSummary;
import net.boomerangplatform.mongo.entity.FlowTeamConfiguration;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.FlowTeamQuotas;
import net.boomerangplatform.mongo.model.UserType;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.service.crud.TeamService;

@RestController
@RequestMapping("/flow")
public class TeamController {

  @Autowired
  private TeamService flowTeamService;

  @Autowired
  private UserIdentityService userIdentityService;

  @PostMapping(value = "/teams")
  public void createCiTeam(@RequestBody CreateFlowTeam createCiTeamRequest) {
    flowTeamService.createFlowTeam(createCiTeamRequest.getCreatedGroupId());
  }

  @GetMapping(value = "/teams")
  public List<TeamWorkflowSummary> getTeams() {
    final FlowUserEntity user = userIdentityService.getCurrentUser();
    if (user == null) {
      return new LinkedList<>();
    } else if (user.getType() == UserType.admin) {
      return flowTeamService.getAllTeams();
    } else {
      return flowTeamService.getUserTeams(user);
    }
  }

  @GetMapping(value = "/teams/{teamId}/properties")
  public List<FlowTeamConfiguration> getAllTeamProperties(@PathVariable String teamId) {
    return flowTeamService.getAllTeamProperties(teamId);
  }

  @DeleteMapping(value = "/teams/{teamId}/properties/{configurationId}")
  public void deleteTeamProperty(@PathVariable String teamId,
      @PathVariable String configurationId) {
    flowTeamService.deleteTeamProperty(teamId, configurationId);
  }

  @PatchMapping(value = "/teams/{teamId}/properties/{configurationId}")
  public List<FlowTeamConfiguration> updateTeamProperty(@PathVariable String teamId,
      @RequestBody FlowTeamConfiguration property, @PathVariable String configurationId) {
    return flowTeamService.updateTeamProperty(teamId, property);
  }

  @PostMapping(value = "/teams/{teamId}/properties")
  public FlowTeamConfiguration createNewTeamProperty(@PathVariable String teamId,
      @RequestBody FlowTeamConfiguration property) {
    return flowTeamService.createNewTeamProperty(teamId, property);
  }

  @GetMapping(value = "/teams/{teamId}/quotas")
  public FlowTeamQuotas getTeamQuotas(@PathVariable String teamId) {
    return flowTeamService.getTeamQuotas(teamId);
  }
}
