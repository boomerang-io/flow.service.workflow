package io.boomerang.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowSettings;
import io.boomerang.model.FlowWebhookResponse;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.RequestFlowExecution;
import io.boomerang.model.WorkflowShortSummary;
import io.boomerang.model.eventing.EventResponse;
import io.boomerang.mongo.model.internal.InternalTaskRequest;
import io.boomerang.mongo.model.internal.InternalTaskResponse;
import io.boomerang.service.EventProcessor;
import io.boomerang.service.WebhookService;
import io.boomerang.service.crud.ConfigurationService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.TaskService;
import io.cloudevents.v1.CloudEventImpl;

@RestController
@RequestMapping("/internal")
public class InternalController {

  @Autowired
  private TaskService taskService;

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private EventProcessor eventProcessor;

  @Autowired
  private WebhookService webhookService;

  @Autowired
  private ConfigurationService configurationService;

  @PostMapping(value = "/task/start")
  public void startTask(@RequestBody InternalTaskRequest request) {
    taskService.createTask(request);
  }

  @PostMapping(value = "/task/end")
  public void endTask(@RequestBody InternalTaskResponse request) {
    taskService.endTask(request);
  }

  @GetMapping(value = "/workflows")
  @Deprecated
  public List<WorkflowShortSummary> getAllWorkflows() {
    return workflowService.getWorkflowShortSummaryList();
  }

  @GetMapping(value = "/system-workflows")
  @Deprecated
  public List<WorkflowShortSummary> getAllSystemworkflows() {
    return workflowService.getSystemWorkflowShortSummaryList();
  }

  @PutMapping(value = "/workflow/event", consumes = "application/cloudevents+json; charset=utf-8")
  public ResponseEntity<CloudEventImpl<EventResponse>> acceptEvent(
      @RequestHeader Map<String, Object> headers, @RequestBody JsonNode payload) {
    return ResponseEntity.ok(eventProcessor.processHTTPEvent(headers, payload));
  }

  @PostMapping(value = "/workflow/{id}/validateToken", consumes = "application/json; charset=utf-8")
  public ResponseEntity<HttpStatus> validateToken(@PathVariable String id,
      @RequestBody GenerateTokenResponse tokenPayload) {
    return workflowService.validateWorkflowToken(id, tokenPayload);
  }

  @PostMapping(value = "/webhook/payload", consumes = "application/json; charset=utf-8")
  public FlowWebhookResponse submitWebhookEvent(@RequestBody RequestFlowExecution request) {
    return webhookService.submitWebhookEvent(request);
  }


  @GetMapping(value = "/webhook/status/{activityId}")
  @Deprecated
  public FlowActivity getWebhookStatus(@PathVariable String activityId) {
    return webhookService.getFlowActivity(activityId);
  }

  @DeleteMapping(value = "/webhook/status/{activityId}")
  @Deprecated
  public ResponseEntity<FlowActivity> terminateActivity(@PathVariable String activityId) {
    return webhookService.terminateActivity(activityId);
  }

  @GetMapping(value = "/workflow/settings")
  @Deprecated
  public List<FlowSettings> getAppConfiguration() {
    return configurationService.getAllSettings();
  }

  @PutMapping(value = "/workflow/settings")
  @Deprecated
  public List<FlowSettings> updateSettings(@RequestBody List<FlowSettings> settings) {
    return configurationService.updateSettings(settings);
  }

}
