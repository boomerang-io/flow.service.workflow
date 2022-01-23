package io.boomerang.controller.api;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
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
import io.boomerang.model.FlowTeam;
import io.boomerang.model.FlowUser;
import io.boomerang.model.TeamQueryResult;
import io.boomerang.model.WorkflowQuotas;
import io.boomerang.model.profile.SortSummary;
import io.boomerang.mongo.model.Quotas;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.mongo.service.FlowUserService;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.service.crud.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1")
@Tag(name = "Team Management", description = "List, Create, update and delete teams.")
public class TeamsV1Controller {

  @Value("${flow.externalUrl.team}")
  private String flowExternalUrlTeam;

  @Autowired
  private TeamService teamService;

  @Autowired
  private FlowUserService flowUserService;


  @GetMapping(value = "/teams")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Search for flow team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<TeamQueryResult> getTeams(
      @RequestParam(required = false) String query,
      @RequestParam(defaultValue = "ASC") Direction order,
      @RequestParam(required = false) String sort, 
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {

    SortSummary sortSummary = new SortSummary();
    sortSummary.setProperty("name");
    sortSummary.setDirection(Direction.ASC.toString());

    Sort pagingSort = Sort.by(Direction.ASC, "name");

    if (StringUtils.isNotBlank(sort)) {
      Direction direction = order == null ? Direction.ASC : order;
      sortSummary.setDirection(direction.toString());
      sortSummary.setProperty(sort);

      pagingSort = Sort.by(direction, sort);
    }
    final Pageable pageable = PageRequest.of(page, size, pagingSort);
    TeamQueryResult response = teamService.getAllAdminTeams(pageable);
    response.setupSortSummary(sortSummary);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/teams")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Create flow team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<FlowTeam> addTeam(@RequestBody FlowTeam flowTeam) {


    if (isTeamManagementAvaliable()) {

      List<String> userEmails = new ArrayList<>();
      if (flowTeam.getUsers() != null && !flowTeam.getUsers().isEmpty()) {
        for (FlowUser user : flowTeam.getUsers()) {
          if (!userEmails.contains(user.getEmail().toLowerCase())) {
            userEmails.add(user.getEmail().toLowerCase());
          } else {
            return ResponseEntity.badRequest().build();
          }
        }
      }
      String teamName = flowTeam.getName();
      FlowTeam team = teamService.createStandaloneTeam(teamName, flowTeam.getQuotas());

      List<String> userIdsToAdd = new ArrayList<>();
      if (flowTeam.getUsers() != null && !flowTeam.getUsers().isEmpty()) {
        for (FlowUser flowUser : flowTeam.getUsers()) {
          if (flowUserService.getUserWithEmail(flowUser.getEmail()) != null) {
            userIdsToAdd.add(flowUserService.getUserWithEmail(flowUser.getEmail()).getId());
          } else {
            String[] userName = flowUser.getName().split(" ", 2);

            userIdsToAdd.add(flowUserService.getOrRegisterUser(flowUser.getEmail(), userName[0],
                userName[1], flowUser.getType()).getId());

          }
          teamService.updateTeamMembers(team.getId(), userIdsToAdd);
        }
      }

      return ResponseEntity.ok(teamService.getTeamByIdDetailed(team.getId()));
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping(value = "/teams/{teamId}")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Delete flow team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> addTeam(@PathVariable String teamId) {
    if (isTeamManagementAvaliable()) {
      teamService.deactivateTeam(teamId);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @PatchMapping(value = "/teams/{teamId}/members")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Update flow team members")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void updateTeamMembers(@PathVariable String teamId,
      @RequestBody List<String> teamMembers) {
    if (isTeamManagementAvaliable()) {
      teamService.updateTeamMembers(teamId, teamMembers);
    }
  }

  @PutMapping(value = "/teams/{teamId}")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Update team details")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void updateTeamMembers(@PathVariable String teamId, @RequestBody FlowTeam flow) {
    if (isTeamManagementAvaliable()) {
      teamService.updateTeam(teamId, flow);
    }
  }

  @GetMapping(value = "/teams/{teamId}/quotas")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Get currrent team quotas")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public WorkflowQuotas getTeamQuotas(@PathVariable String teamId) {
    return teamService.getTeamQuotas(teamId);
  }

  @PutMapping(value = "/teams/{teamId}/quotas")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Update Quotas for a team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Quotas updateQuotasForTeam(@PathVariable String teamId, @RequestBody Quotas quotas) {
    return teamService.updateQuotasForTeam(teamId, quotas);
  }

  private boolean isTeamManagementAvaliable() {
    return flowExternalUrlTeam.isBlank();
  }
}
