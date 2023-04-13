package io.boomerang.controller.teams;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.FlowUserProfile;
import io.boomerang.model.UserQueryResult;
import io.boomerang.model.profile.SortSummary;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.service.UserIdentityService;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.User;
import io.boomerang.v4.service.TeamService;


@RestController
@RequestMapping("/workflow/manage")
public class ManagementController {

  @Value("${flow.externalUrl.team}")
  private String flowExternalUrlTeam;

  @Value("${flow.externalUrl.user}")
  private String flowExternalUrlUser;

  @Autowired
  private TeamService teamService;

  @Autowired
  private UserIdentityService userIdentityService;

  @GetMapping(value = "/users/{userId}")
  public FlowUserProfile getUserProfile(@PathVariable String userId) {
    return userIdentityService.getFullUserProfile(userId);
  }

  @PatchMapping(value = "/users/{userId}")
  public void updateFlowUser(@PathVariable String userId, @RequestBody User flowUser) {
    if (isUserManagementAvaliable()) {
      userIdentityService.updateFlowUser(userId, flowUser);
    }
  }

  @DeleteMapping(value = "/users/{userId}")
  public void deleteFlowUser(@PathVariable String userId) {
    if (isUserManagementAvaliable()) {
      userIdentityService.deleteFlowUser(userId);
    }
  }

  @PostMapping(value = "/users")
  public UserEntity addUser(@RequestBody User flowUser) {
    if (isUserManagementAvaliable()) {
      return userIdentityService.addFlowUser(flowUser);
    }
    return new UserEntity();
  }


  @GetMapping(value = "/users")
  public UserQueryResult getUsers(@RequestParam(required = false) String query,
      @RequestParam(defaultValue = "ASC") Direction order,
      @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {

    if (isUserManagementAvaliable()) {
      Sort pagingSort = Sort.by(Direction.ASC, "firstLoginDate");

      SortSummary sortSummary = new SortSummary();
      sortSummary.setProperty("firstLoginDate");
      sortSummary.setDirection(Direction.ASC.toString());

      if (StringUtils.isNotBlank(sort)) {
        Direction direction = order == null ? Direction.ASC : order;
        sortSummary.setDirection(direction.toString());
        sortSummary.setProperty(sort);
        pagingSort = Sort.by(direction, sort);
      }

      final Pageable pageable = PageRequest.of(page, size, pagingSort);
      if (StringUtils.isNotBlank(query)) {
        UserQueryResult result = userIdentityService.getUserViaSearchTerm(query, pageable);
        result.setupSortSummary(sortSummary);
        return result;
      } else {
        UserQueryResult result = userIdentityService.getUsers(pageable);
        result.setupSortSummary(sortSummary);
        return result;
      }
    }
    return new UserQueryResult();
  }

  @PostMapping(value = "/teams")
  public Team addTeam(@RequestBody Team flowTeam) {
    if (isTeamManagementAvaliable()) {
      String teamName = flowTeam.getName();
      return teamService.createStandaloneTeam(teamName, flowTeam.getQuotas());
    }
    return new Team();
  }

  @DeleteMapping(value = "/teams/{teamId}")
  public void deactivateTeam(@PathVariable String teamId) {
    if (isTeamManagementAvaliable()) {
      teamService.deactivateTeam(teamId);
    }
  }


  @PatchMapping(value = "/teams/{teamId}/members")
  public void updateTeamMembers(@PathVariable String teamId,
      @RequestBody List<String> teamMembers) {
    if (isTeamManagementAvaliable()) {
      teamService.updateTeamMembers(teamId, teamMembers);
    }
  }

  @PatchMapping("/teams/{teamId}/labels")
  public TeamEntity updateTeamLabels(@PathVariable String teamId,
      @RequestBody List<KeyValuePair> labels) {
    if (isTeamManagementAvaliable()) {
      return teamService.updateTeamLabels(teamId, labels);
    }
    return new TeamEntity();
  }

  @PutMapping(value = "/teams/{teamId}")
  public void updateTeamMembers(@PathVariable String teamId, @RequestBody Team flow) {
    if (isTeamManagementAvaliable()) {
      teamService.updateTeam(teamId, flow);
    }
  }

  private boolean isUserManagementAvaliable() {
    return flowExternalUrlUser.isBlank();
  }

  private boolean isTeamManagementAvaliable() {
    return flowExternalUrlTeam.isBlank();
  }

}
