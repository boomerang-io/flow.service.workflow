package io.boomerang.v4.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
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
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.data.model.TeamParameter;
import io.boomerang.v4.model.ApproverGroupCreateRequest;
import io.boomerang.v4.model.ApproverGroup;
import io.boomerang.v4.model.CreateTeamRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v2/team")
public class TeamV2Controller {

  @Autowired
  private TeamService teamService;  
  
//
//  @Autowired
//  private UserIdentityService userIdentityService;
//  
//  @Autowired
//  private UserValidationService userValidationService;
//  
//  @Autowired
//  private WorkflowService workflowService;

  @GetMapping(value = "/query")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Search for Teams")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<Team> getTeams(@Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status", description = "List of statuses to filter for. Defaults to all.",
          example = "active,inactive",
          required = false) @RequestParam(required = false) Optional<List<String>> status,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
    return teamService.query(page, limit, sort, labels, status);
  }
  
  @GetMapping(value = "/{teamId}")
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Get teams")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Team> getTeam(
      @Parameter(name = "teamId",
      description = "ID of Team",
      required = true) @PathVariable String teamId) {
    return teamService.get(teamId);
  }
  
  @PostMapping(value = "/")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Create new team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Team> createTeam(@RequestBody CreateTeamRequest createTeamRequest) {
    return teamService.create(createTeamRequest);
  }
  
  @PatchMapping(value = "/")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Create new team")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Team> updateTeam(@RequestBody CreateTeamRequest createTeamRequest) {
    return teamService.updateTeam(createTeamRequest);
  }
  
  @PutMapping(value = "/{teamId}/enable")
  @Operation(summary = "Enable a Team")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> enableWorkflow(
      @Parameter(name = "teamId",
      description = "ID of Team",
      required = true) @PathVariable String teamId) {
    return teamService.enable(teamId);
  }

  @PutMapping(value = "/{teamId}/disable")
  @Operation(summary = "Disable a Team")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> disableWorkflow(@Parameter(name = "teamId",
      description = "ID of Team", required = true) @PathVariable String teamId) {
    return teamService.disable(teamId);
  }

  @GetMapping(value = "/{teamId}/parameters")
  public ResponseEntity<List<TeamParameter>> getParameters(
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
  public ResponseEntity<TeamParameter> patchParameter(
      @Parameter(name = "teamId", description = "ID of Team",
          required = true) @PathVariable String teamId,
      @RequestBody TeamParameter parameter) {
    return teamService.updateParameter(teamId, parameter);
  }

  @PostMapping(value = "/{teamId}/parameters")
  public ResponseEntity<TeamParameter> createNewTeamProperty(
      @Parameter(name = "teamId", description = "ID of Team",
      required = true) @PathVariable String teamId,
  @RequestBody TeamParameter parameter) {
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
      @RequestBody ApproverGroupCreateRequest createApproverGroupRequest) {
    return teamService.createApproverGroup(teamId, createApproverGroupRequest);
  }
  
  @PutMapping(value = "/{teamId}/approvers/{groupId}")
  public ApproverGroup updateApproverGroup(@PathVariable String teamId, @PathVariable String groupId, @RequestBody ApproverGroupCreateRequest updateApproverGroup) {
    return teamService.updateApproverGroup(teamId, groupId, updateApproverGroup);
  }
//  
//  @GetMapping(value = "/{teamId}/approvers/{groupId}")
//  public ApproverGroupResponse getSingleAproverGroup(@PathVariable String teamId, @PathVariable String groupId) {
//    return teamService.getSingleAproverGroup(teamId, groupId);
//  }
  
  @DeleteMapping(value = "/{teamId}/approvers/{name}")
  public void deleteApproverGroup(@PathVariable String teamId,@PathVariable String name) {
    teamService.deleteApproverGroup(teamId, name);
  }
  
  //TODO: confirm if we need this now that Users are returned on the Team Detail
//  @GetMapping(value = "/teams/{teamId}/members")
//  public List<User> getTeamMembers(@PathVariable String teamId) {
//    return teamService.getTeamMembers(teamId);
//  }
  
//  @GetMapping(value = "/teams/{teamId}/workflows")
//  public List<WorkflowSummary> getTeamWorkflows(@PathVariable String teamId) {
//    return workflowService.getWorkflowsForTeam(teamId);
//  }
}
