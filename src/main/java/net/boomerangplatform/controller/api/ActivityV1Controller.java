package net.boomerangplatform.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.security.interceptors.AuthenticationScope;
import net.boomerangplatform.security.interceptors.Scope;
import net.boomerangplatform.service.WebhookService;

@RequestMapping("/workflow/apis/v1")
public class ActivityV1Controller {

  @Autowired
  private WebhookService webhookService;

  @GetMapping(value = "/activity/{activityId}")
  @AuthenticationScope(scopes = {Scope.global,Scope.team, Scope.user})
  public FlowActivity getWebhookStatus(@PathVariable String activityId) {
    return webhookService.getFlowActivity(activityId);
  }

  @DeleteMapping(value = "/activity/{activityId}")
  @AuthenticationScope(scopes = {Scope.global,Scope.team, Scope.user})
  public ResponseEntity<FlowActivity> terminateActivity(@PathVariable String activityId) {
    return webhookService.terminateActivity(activityId);
  }
}
