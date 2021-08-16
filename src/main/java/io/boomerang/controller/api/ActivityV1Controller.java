package io.boomerang.controller.api;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.FlowActivity;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.security.interceptors.Scope;
import io.boomerang.service.WebhookService;
import io.boomerang.service.crud.FlowActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1")
@Tag(name = "Activity Management",
    description = "Submit requests to execute workflows and provide the ability to search and retrieve workflow activities.")
public class ActivityV1Controller {

  @Autowired
  private WebhookService webhookService;

  @Autowired
  private FlowActivityService activityService;

  @GetMapping(value = "/activites")
  @AuthenticationScope(scopes = {Scope.global, Scope.team, Scope.user})
  @Operation(summary = "Search for workflow execution activites")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<FlowActivity> searchForActivity(@Parameter(
      name = "labels",
      description = "Comma separated list of key value pairs which is url encoded. For example Organization=IBM,customKey=test would be encoded as Organization%3DIBM%2CcustomKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<String> labels,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Pageable pageable = PageRequest.of(page, limit);
    return activityService.findActivty(pageable, labels);
  }

  @GetMapping(value = "/activity/{activityId}")
  @AuthenticationScope(scopes = {Scope.global, Scope.team, Scope.user})
  @Operation(summary = "Retrieve a single workfloow execution")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public FlowActivity getWebhookStatus(@PathVariable String activityId) {
    return webhookService.getFlowActivity(activityId);
  }

  @DeleteMapping(value = "/activity/{activityId}")
  @AuthenticationScope(scopes = {Scope.global, Scope.team, Scope.user})
  @Operation(summary = "Cancel a workflow execution")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<FlowActivity> terminateActivity(@PathVariable String activityId) {
    return webhookService.terminateActivity(activityId);
  }
}
