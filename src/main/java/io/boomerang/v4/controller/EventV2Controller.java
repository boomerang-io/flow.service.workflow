package io.boomerang.v4.controller;

import java.util.List;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowWebhookResponse;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.RequestFlowExecution;
import io.boomerang.model.WFETriggerResponse;
import io.boomerang.model.eventing.EventResponse;
import io.boomerang.v4.model.Settings;
import io.cloudevents.v1.CloudEventImpl;
import io.swagger.v3.oas.annotations.tags.Tag;

/*
 * 
 */
@RestController
@RequestMapping("/api/v2")
@Tag(name = "Event and Webhook Management",
    description = "Listen for Events or Webhook requests to execute Workflows and provide the ability to resolve Wait For Event TaskRuns.")
public class EventV2Controller {

//  @PutMapping(value = "/event", consumes = "application/cloudevents+json; charset=utf-8")
//  public ResponseEntity<CloudEventImpl<EventResponse>> acceptEvent(
//      @RequestHeader Map<String, Object> headers, @RequestBody JsonNode payload) {
//    return ResponseEntity.ok(eventProcessor.processHTTPEvent(headers, payload));
//  }
//
//  @PostMapping(value = "/webhook/payload", consumes = "application/json; charset=utf-8")
//  public FlowWebhookResponse submitWebhookEvent(@RequestBody RequestFlowExecution request) {
//    return webhookService.submitWebhookEvent(request);
//  }
//
//  @GetMapping(value = "/webhook/status/{activityId}")
//  @Deprecated
//  public FlowActivity getWebhookStatus(@PathVariable String activityId) {
//    return webhookService.getFlowActivity(activityId);
//  }
//
//  @DeleteMapping(value = "/webhook/status/{activityId}")
//  @Deprecated
//  public ResponseEntity<FlowActivity> terminateActivity(@PathVariable String activityId) {
//    return webhookService.terminateActivity(activityId);
//  }
}
