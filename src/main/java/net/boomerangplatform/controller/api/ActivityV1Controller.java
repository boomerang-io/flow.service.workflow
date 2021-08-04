package net.boomerangplatform.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.security.interceptors.AuthenticationScope;
import net.boomerangplatform.security.interceptors.Scope;
import net.boomerangplatform.service.WebhookService;

@RestController
@RequestMapping("/apis/v1")
@Tag(name = "Activity Management",
    description = "Provides the ability to submit, search and check workflow activities.")
public class ActivityV1Controller {

  @Autowired
  private WebhookService webhookService;

  @GetMapping(value = "/activity/{activityId}")
  @AuthenticationScope(scopes = {Scope.global, Scope.team, Scope.user})
  @Operation(summary = "Retrieve workflow status")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public FlowActivity getWebhookStatus(@PathVariable String activityId) {
    return webhookService.getFlowActivity(activityId);
  }

  @DeleteMapping(value = "/activity/{activityId}")
  @AuthenticationScope(scopes = {Scope.global, Scope.team, Scope.user})
  @Operation(summary = "Terminate a workflow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<FlowActivity> terminateActivity(@PathVariable String activityId) {
    return webhookService.terminateActivity(activityId);
  }
}
