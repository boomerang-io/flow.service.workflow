package io.boomerang.controller.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.WorkflowShortSummary;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.security.interceptors.Scope;
import io.boomerang.service.crud.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1")
@Tag(name = "Workflow Management",
    description = "Provides the ability to list all workflows and system workflows")
public class WorkflowsV1Controller {

  @Autowired
  private WorkflowService workflowService;

  @GetMapping(value = "/workflows")
  @AuthenticationScope(scopes = {Scope.global})
  @Operation(summary = "List all workflows")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<WorkflowShortSummary> getAllWorkflows(
      @Parameter(name = "x-access-token",
          required = true) @RequestHeader("x-access-token") String token) {
    return workflowService.getWorkflowShortSummaryList();
  }

  @GetMapping(value = "/system-workflows")
  @AuthenticationScope(scopes = {Scope.global})
  @Operation(summary = "List all system workflows")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<WorkflowShortSummary> getAllSystemworkflows(
      @Parameter(name = "x-access-token",
          required = true) @RequestHeader("x-access-token") String token) {
    return workflowService.getSystemWorkflowShortSummaryList();
  }
}
