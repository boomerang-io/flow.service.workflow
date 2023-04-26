package io.boomerang.v4.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
