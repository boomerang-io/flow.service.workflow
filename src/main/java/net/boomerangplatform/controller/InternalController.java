package net.boomerangplatform.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import io.cloudevents.v1.CloudEventImpl;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.GenerateTokenResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.model.WorkflowShortSummary;
import net.boomerangplatform.model.eventing.EventResponse;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.service.EventProcessor;
import net.boomerangplatform.service.WebhookService;
import net.boomerangplatform.service.crud.WorkflowService;
import net.boomerangplatform.service.refactor.TaskService;

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

  @PostMapping(value = "/task/start")
  public void startTask(@RequestBody InternalTaskRequest request) {
    taskService.createTask(request);
  }

  @PostMapping(value = "/task/end")
  public void endTask(@RequestBody InternalTaskResponse request) {
    taskService.endTask(request);
  }


  @GetMapping(value = "/workflows")
  public List<WorkflowShortSummary> getAllWorkflows() {
    return workflowService.getWorkflowShortSummaryList();
  }
  
  @PutMapping(value = "/workflow/event", consumes = "application/cloudevents+json; charset=utf-8")
  public ResponseEntity<CloudEventImpl<EventResponse>> acceptEvent(@RequestHeader Map<String, Object> headers, @RequestBody JsonNode payload) {
    eventProcessor.processHTTPEvent(headers, payload);

    return ResponseEntity.ok(eventProcessor.processHTTPEvent(headers, payload));
  }
  
  @PostMapping(value = "/workflow/{id}/validateToken", consumes = "application/json; charset=utf-8")
  public ResponseEntity<HttpStatus> validateToken(@PathVariable String id, @RequestBody GenerateTokenResponse tokenPayload){ 
    return workflowService.validateWorkflowToken(id, tokenPayload);
  }
  
  @PostMapping(value = "/webhook/payload", consumes = "application/json; charset=utf-8")
  public FlowWebhookResponse submitWebhookEvent(@RequestBody RequestFlowExecution request) {
    return webhookService.submitWebhookEvent(request);
  }
  

  @GetMapping(value = "/webhook/status/{activityId}")
  public FlowActivity getWebhookStatus(@PathVariable String activityId
    ) {
    return webhookService.getFlowActivity(activityId);
  }
}
