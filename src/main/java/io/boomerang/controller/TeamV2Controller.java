package io.boomerang.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.data.model.Quotas;
import io.boomerang.model.Team;
import io.boomerang.model.TeamMember;
import io.boomerang.model.TeamNameCheckRequest;
import io.boomerang.model.TeamRequest;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.security.model.Role;
import io.boomerang.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/team")
@Tag(name = "Team Management",
description = "Manage Teams, Team Members, Quotas, ApprovalGroups and Parameters.")
public class TeamV2Controller {

  @Autowired
  private TeamService teamService;
  
  @PostMapping(value = "/validate-name")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Validate team name and check uniqueness.")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "422", description = "Name is already taken"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<?> validateTeamName(@RequestBody TeamNameCheckRequest request) {
    return teamService.validateName(request);
  }
  
  @GetMapping(value = "/{team}")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  @Operation(summary = "Get team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Team getTeam(
      @Parameter(name = "team",
          description = "Team as owner reference.",
          example = "my-amazing-team",
          required = true) @PathVariable String team) {
    return teamService.get(team);
  }

  @GetMapping(value = "/query")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  @Operation(summary = "Search for Teams")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<Team> getTeams(@Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses", description = "List of statuses to filter for. Defaults to all.",
          example = "active,inactive",
          required = false) @RequestParam(required = false) Optional<List<String>> statuses,
      @Parameter(name = "teams", description = "List of Team names to filter for.", example = "my-amazing-team,boomerangs-return",
      required = false) @RequestParam(required = false) Optional<List<String>> names,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "order", description = "Ascending or Descending (default) order", example = "0",
  required = false) @RequestParam(defaultValue = "DESC") Optional<Direction> order,
  @Parameter(name = "sort", description = "The element to sort on", example = "0",
  required = false) @RequestParam(defaultValue = "name") Optional<String> sort) {
    return teamService.query(page, limit, order, sort, labels, statuses, names);
  }
  
  @PostMapping(value = "")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.global})
  @Operation(summary = "Create new team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Team createTeam(@RequestBody TeamRequest request) {
    return teamService.create(request);
  }
  
  @PatchMapping(value = "/{team}")
  @AuthScope(action = PermissionAction.WRITE, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  @Operation(summary = "Patch or update a team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Team updateTeam(@Parameter(name = "team",
      description = "ID of Team", required = true) @PathVariable String team, @RequestBody TeamRequest request) {
    return teamService.patch(team, request);
  }

  @DeleteMapping(value = "/{team}")
  @Operation(summary = "Delete a team")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void deleteWorkflow(@Parameter(name = "team",
      description = "ID of Team", required = true) @PathVariable String team) {
    teamService.delete(team);
  }
  
  @DeleteMapping(value = "/{team}/members")
  @AuthScope(action = PermissionAction.DELETE, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  public void removeMembers(
      @Parameter(name = "team",
      description = "Team as owner reference.",
      example = "my-amazing-team", required = true) @PathVariable String team, @RequestBody List<TeamMember> request) {
      teamService.removeMembers(team, request);
  }
  
  @DeleteMapping(value = "/{team}/leave")
  @AuthScope(action = PermissionAction.ACTION, scope = PermissionScope.TEAM, types = {AuthType.user, AuthType.session})
  public void leave(
      @Parameter(name = "team",
      description = "Team as owner reference.",
      required = true) @PathVariable String team) {
      teamService.leave(team);
  }

  @DeleteMapping(value = "/{team}/parameters")
  @AuthScope(action = PermissionAction.DELETE, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  public void deleteParameters(
      @Parameter(name = "team",
      description = "Team as owner reference.",
      required = true) @PathVariable String team,
      @RequestBody List<String> keys) {
    teamService.deleteParameters(team, keys);
  }
  
  @DeleteMapping(value = "/{team}/approvers")
  @AuthScope(action = PermissionAction.DELETE, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  public void deleteApproverGroup(
      @Parameter(name = "team",
      description = "Team as owner reference.",
      required = true) @PathVariable String team,
      @RequestBody List<String> names) {
    teamService.deleteApproverGroups(team, names);
  }

  @DeleteMapping(value = "/{team}/quotas")
  @AuthScope(action = PermissionAction.DELETE, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  public void resetQuotas(
      @Parameter(name = "team",
      description = "Team as owner reference.",
      required = true) @PathVariable String team) {
    teamService.deleteCustomQuotas(team);
  }

  @GetMapping(value = "/quotas/default")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  public ResponseEntity<Quotas> getDefaultQuotas() {
    return teamService.getDefaultQuotas();
  }

  @GetMapping(value = "/roles")
  @AuthScope(action = PermissionAction.READ, scope = PermissionScope.TEAM, types = {AuthType.session, AuthType.user, AuthType.team, AuthType.global})
  public ResponseEntity<List<Role>> getRoles() {
    return teamService.getRoles();
  }
}
