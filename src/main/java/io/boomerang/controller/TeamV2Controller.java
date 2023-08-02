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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.model.Role;
import io.boomerang.service.TeamService;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.ApproverGroup;
import io.boomerang.v4.model.ApproverGroupRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.TeamNameCheckRequest;
import io.boomerang.v4.model.TeamRequest;
import io.boomerang.v4.model.UserSummary;
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
//@AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Check team name")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "422", description = "Name is already taken"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<?> validateTeamName(@RequestBody TeamNameCheckRequest request) {
    return teamService.validateName(request);
  }

  //TODO - merge back in the profile.
  @GetMapping(value = "/mine")
//  @AuthScope(types = {TokenScope.session, TokenScope.user}, access = TokenAccess.read, object = TokenObject.team)
  @Operation(summary = "Return all my teams")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<Team> getMyTeams(@Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses", description = "List of statuses to filter for. Defaults to all.",
          example = "active,inactive",
          required = false) @RequestParam(required = false) Optional<List<String>> statuses,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "order", description = "Ascending or Descending (default) order", example = "0",
  required = false) @RequestParam(defaultValue = "DESC") Optional<Direction> order,
  @Parameter(name = "sort", description = "The element to sort on", example = "0",
  required = false) @RequestParam(defaultValue = "name") Optional<String> sort) {
    return teamService.mine(page, limit, order, sort, labels, statuses);
  }

  @GetMapping(value = "/query")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Search for Teams")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<Team> getTeams(@Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "statuses", description = "List of statuses to filter for. Defaults to all.",
          example = "active,inactive",
          required = false) @RequestParam(required = false) Optional<List<String>> statuses,
      @Parameter(name = "ids", description = "List of ids to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> ids,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "order", description = "Ascending or Descending (default) order", example = "0",
  required = false) @RequestParam(defaultValue = "DESC") Optional<Direction> order,
  @Parameter(name = "sort", description = "The element to sort on", example = "0",
  required = false) @RequestParam(defaultValue = "name") Optional<String> sort) {
    return teamService.query(page, limit, order, sort, labels, statuses, ids);
  }
  
  @GetMapping(value = "/{teamId}")
//  @AuthenticationScope(scopes = {TokenPermission.global, TokenPermission.team, TokenPermission.user})
  @Operation(summary = "Get teams")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Team> getTeam(
      @Parameter(name = "teamId",
      description = "ID of Team",
      required = true) @PathVariable String teamId) {
    return teamService.get(teamId);
  }
  
  @PostMapping(value = "")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Create new team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Team> createTeam(@RequestBody TeamRequest request) {
    return teamService.create(request);
  }
  
  @PatchMapping(value = "")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Patch or update a team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Team> updateTeam(@RequestBody TeamRequest request) {
    return teamService.patch(request);
  }
  
  @PatchMapping(value = "/{teamId}/members")
  public ResponseEntity<List<UserSummary>> addMembers(@Parameter(name = "teamId",
      description = "ID of Team", required = true) @PathVariable String teamId, @RequestBody List<UserSummary> request) {
    return teamService.addMembers(teamId, request);
  }
  
  @DeleteMapping(value = "/{teamId}/members")
  public void removeMembers(@Parameter(name = "teamId",
      description = "ID of Team", required = true) @PathVariable String teamId, @RequestBody List<UserSummary> request) {
      teamService.removeMembers(teamId, request);
  }

  @GetMapping(value = "/{teamId}/parameters")
  public ResponseEntity<List<AbstractParam>> getParameters(
      @Parameter(name = "teamId",
      description = "ID of Team",
      required = true) @PathVariable String teamId) {
    return teamService.getParameters(teamId);
  }

  @DeleteMapping(value = "/{teamId}/parameters/{key}")
  public void deleteTeamProperty(
      @Parameter(name = "teamId",
      description = "ID of Team",
      required = true) @PathVariable String teamId,
      @Parameter(name = "key", description = "The parameters unique key.",
      example = "my-parameter",
      required = true) @PathVariable String key) {
    teamService.deleteParameter(teamId, key);
  }

  @PatchMapping(value = "/{teamId}/parameters")
  public ResponseEntity<AbstractParam> patchParameter(
      @Parameter(name = "teamId", description = "ID of Team",
          required = true) @PathVariable String teamId,
      @RequestBody AbstractParam parameter) {
    return teamService.updateParameter(teamId, parameter);
  }

  @PostMapping(value = "/{teamId}/parameters")
  public ResponseEntity<AbstractParam> createNewTeamProperty(
      @Parameter(name = "teamId", description = "ID of Team",
      required = true) @PathVariable String teamId,
  @RequestBody AbstractParam parameter) {
    return teamService.createParameter(teamId, parameter);
  }

  @GetMapping(value = "/{teamId}/quotas")
  public ResponseEntity<CurrentQuotas> getQuotas(
      @Parameter(name = "teamId", description = "ID of Team",
      required = true) @PathVariable String teamId) {
    return teamService.getQuotas(teamId);
  }

  @PutMapping(value = "/{teamId}/quotas/reset")
  public ResponseEntity<Quotas> resetQuotas(@Parameter(name = "teamId", description = "ID of Team",
      required = true) @PathVariable String teamId) {
    return teamService.resetQuotas(teamId);
  }

  @PatchMapping(value = "/{teamId}/quotas")
  public ResponseEntity<Quotas> updateTeamQuotas(@Parameter(name = "teamId", description = "ID of Team",
      required = true) @PathVariable String teamId,
      @RequestBody Quotas quotas) {
    return teamService.patchQuotas(teamId, quotas);
  }

  @GetMapping(value = "/quotas/default")
  public ResponseEntity<Quotas> getDefaultQuotas() {
    return teamService.getDefaultQuotas();
  }

  @GetMapping(value = "/{teamId}/approvers")
  public ResponseEntity<List<ApproverGroup>> getApproverGroups(@PathVariable String teamId) {
    return teamService.getApproverGroups(teamId);
  }
  
  @PostMapping(value = "/{teamId}/approvers")
  public ResponseEntity<ApproverGroup> createApproverGroup(@Parameter(name = "teamId", description = "ID of Team",
      required = true) @PathVariable String teamId,
      @RequestBody ApproverGroupRequest request) {
    return teamService.createApproverGroup(teamId, request);
  }
  
  @PutMapping(value = "/{teamId}/approvers")
  public ResponseEntity<ApproverGroup> updateApproverGroup(@Parameter(name = "teamId", description = "ID of Team",
      required = true) @PathVariable String teamId,
      @RequestBody ApproverGroupRequest request) {
    return teamService.updateApproverGroup(teamId, request);
  }
  
  @DeleteMapping(value = "/{teamId}/approvers/{name}")
  public void deleteApproverGroup(@PathVariable String teamId,@PathVariable String name) {
    teamService.deleteApproverGroup(teamId, name);
  }  

  @GetMapping(value = "/roles")
  public ResponseEntity<List<Role>> getRoles() {
    return teamService.getRoles();
  }
}
