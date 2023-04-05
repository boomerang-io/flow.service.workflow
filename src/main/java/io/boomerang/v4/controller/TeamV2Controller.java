package io.boomerang.v4.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.v4.model.CreateTeamRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.User;
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
  
  @PutMapping(value = "/{teamId}/enable")
  @Operation(summary = "Enable a Team")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> enableWorkflow(@Parameter(name = "teamId",
      description = "ID of Team", required = true) @PathVariable String teamId) {
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

//
//  @GetMapping(value = "/teams/{teamId}/properties")
//  public List<FlowTeamConfiguration> getAllTeamProperties(@PathVariable String teamId) {
//    userValidationService.validateUserForTeam(teamId);
//    return flowTeamService.getAllTeamProperties(teamId);
//  }
//  
//  @GetMapping(value = "/teams/{teamId}/members")
//  public List<TeamMember> getTeamMembers(@PathVariable String teamId) {
//    return flowTeamService.getTeamMembers(teamId);
//  }
//
//  @DeleteMapping(value = "/teams/{teamId}/properties/{configurationId}")
//  public void deleteTeamProperty(@PathVariable String teamId,
//      @PathVariable String configurationId) {
//    flowTeamService.deleteTeamProperty(teamId, configurationId);
//  }
//
//  @PatchMapping(value = "/teams/{teamId}/properties/{configurationId}")
//  public List<FlowTeamConfiguration> updateTeamProperty(@PathVariable String teamId,
//      @RequestBody FlowTeamConfiguration property, @PathVariable String configurationId) {
//    return flowTeamService.updateTeamProperty(teamId, property);
//  }
//
//  @PostMapping(value = "/teams/{teamId}/properties")
//  public FlowTeamConfiguration createNewTeamProperty(@PathVariable String teamId,
//      @RequestBody FlowTeamConfiguration property) {
//    userValidationService.validateUserForTeam(teamId);
//    return flowTeamService.createNewTeamProperty(teamId, property);
//  }
//
//  @GetMapping(value = "/teams/{teamId}/quotas")
//  public WorkflowQuotas getTeamQuotas(@PathVariable String teamId) {
//    userValidationService.validateUserForTeam(teamId);
//    return flowTeamService.getTeamQuotas(teamId);
//  }
//
//  @PutMapping(value = "/teams/{teamId}/quotas/default")
//  public WorkflowQuotas resetTeamQuotas(@PathVariable String teamId) {
//    return flowTeamService.resetTeamQuotas(teamId);
//  }
//
//  @PatchMapping(value = "/teams/{teamId}/quotas")
//  public Quotas updateTeamQuotas(@PathVariable String teamId, @RequestBody Quotas quotas) {
//    userValidationService.validateUserForTeam(teamId);
//    return flowTeamService.updateTeamQuotas(teamId, quotas);
//  }
//
//  @PutMapping(value = "/teams/{teamId}/quotas")
//  public Quotas updateQuotasForTeam(@PathVariable String teamId, @RequestBody Quotas quotas) {
//    userValidationService.validateUserForTeam(teamId);
//    return flowTeamService.updateQuotasForTeam(teamId, quotas);
//  }
//
//  @GetMapping(value = "/quotas/default")
//  public Quotas getDefaultQuotas() {
//    return flowTeamService.getDefaultQuotas();
//  }
//
//  @GetMapping(value = "/manage/teams/{teamId}")
//  public FlowTeam getTeam(@PathVariable String teamId) {
//    return flowTeamService.getTeamByIdDetailed(teamId);
//  }
//
//  @GetMapping(value = "/manage/teams")
//  public TeamQueryResult getTeams(@RequestParam(required = false) String query,
//      @RequestParam(defaultValue = "ASC") Direction order,
//      @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page,
//      @RequestParam(defaultValue = "100") int size) {
//
//    SortSummary sortSummary = new SortSummary();
//    sortSummary.setProperty("name");
//    sortSummary.setDirection(Direction.ASC.toString());
//
//    Sort pagingSort = Sort.by(Direction.ASC, "name");
//
//    if (StringUtils.isNotBlank(sort)) {
//      Direction direction = order == null ? Direction.ASC : order;
//      sortSummary.setDirection(direction.toString());
//      sortSummary.setProperty(sort);
//
//      pagingSort = Sort.by(direction, sort);
//    }
//
//    final Pageable pageable = PageRequest.of(page, size, pagingSort);
//
//    TeamQueryResult response = flowTeamService.getAllAdminTeams(pageable);
//    response.setupSortSummary(sortSummary);
//    return response;
//  }
//
//  @DeleteMapping(value = "/teams/{teamId}")
//  public TeamEntity deactivateTeam(@PathVariable String teamId) {
//    return flowTeamService.deactivateTeam(teamId);
//  }
// 
//  @GetMapping(value = "/teams/{teamId}/approvers")
//  public List<ApproverGroupResponse> getApproverGroups(@PathVariable String teamId) {
//    return flowTeamService.getTeamApproverGroups(teamId);
//  }
//  
//  @PostMapping(value = "/teams/{teamId}/approvers")
//  public ApproverGroupResponse createApproverGroup(@PathVariable String teamId, @RequestBody CreateApproverGroupRequest createApproverGroupRequest) {
//    return flowTeamService.createApproverGroup(teamId, createApproverGroupRequest);
//  }
//  
//  @PutMapping(value = "/teams/{teamId}/approvers/{groupId}")
//  public ApproverGroupResponse updateApproverGroup(@PathVariable String teamId, @PathVariable String groupId, @RequestBody CreateApproverGroupRequest updateApproverGroup) {
//    return flowTeamService.updateApproverGroup(teamId, groupId, updateApproverGroup);
//  }
//  
//  @GetMapping(value = "/teams/{teamId}/approvers/{groupId}")
//  public ApproverGroupResponse getSingleAproverGroup(@PathVariable String teamId, @PathVariable String groupId) {
//    return flowTeamService.getSingleAproverGroup(teamId, groupId);
//  }
// 
//  
//  @DeleteMapping(value = "/teams/{teamId}/approvers/{groupId}")
//  public void deleteApproverGroup(@PathVariable String teamId,@PathVariable String groupId) {
//    flowTeamService.deleteApproverGroup(teamId, groupId);
//  }
//  
//  @GetMapping(value = "/teams/{teamId}/workflows")
//  public List<WorkflowSummary> getTeamWorkflows(@PathVariable String teamId) {
//    return workflowService.getWorkflowsForTeam(teamId);
//  }
}
