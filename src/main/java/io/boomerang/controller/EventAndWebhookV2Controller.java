package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.SlackEventPayload;
import io.boomerang.model.WebhookType;
import io.boomerang.service.EventAndWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/*
 *  Handles receiving generic Webhook and CloudEvents as opposed to the WorkflowRun endpoint
 */
@RestController
@RequestMapping("/api/v2")
@Tag(name = "Event and Webhook Management",
    description = "Listen for Events or Webhook requests to execute Workflows and provide the ability to resolve Wait For Event TaskRuns.")
public class EventAndWebhookV2Controller {
  
  private static String STATUS_SUCCESS = "success";

  @Autowired
  private EventAndWebhookService eventAndWebhookService;
  
  /**
   * HTTP Webhook accepting Generic, Slack Events, and Dockerhub subtypes. For Slack and
   * Dockerhub will respond/perform verification challenges.
   * <p>
   * <b>Note:</b> Partial conformance to the specification.
   * 
   * <h4>Specifications</h4>
   * <ul>
   * <li><a href=
   * "https://github.com/cloudevents/spec/blob/master/http-webhook.md">CloudEvents</a></li>
   * <li><a href="https://docs.docker.com/docker-hub/webhooks/">Dockerhub</a></li>
   * <li><a href="https://api.slack.com/events-api">Slack Events API</a></li>
   * <li><a href="https://api.slack.com/events">Slack Events</a></li>
   * </ul>
   * 
   * <h4>Sample</h4>
   * <code>/webhook?workflowId={workflowId}&type={generic|slack|dockerhub}&access_token={access_token}</code>
   */
  @PostMapping(value = "/webhook", consumes = "application/json; charset=utf-8")
  @Operation(summary = "Trigger WorkflowRun via Webhook.")
  public ResponseEntity<?> acceptWebhookEvent(
      @Parameter(name = "workflow",
      description = "Workflow reference the request relates to",
      required = true) @RequestParam(required = true) String workflow,
      @Parameter(name = "type",
      description = "The type of webhook allowing for specialised payloads. Defaults to 'generic'.",
      required = true) @RequestParam(defaultValue = "generic") WebhookType type,
      @RequestBody JsonNode payload) {    
    switch (type) {
      case slack:
        if (payload != null) {
          final String slackType = payload.get("type").asText();
  
          if ("url_verification".equals(slackType)) {
            SlackEventPayload response = new SlackEventPayload();
            final String slackChallenge = payload.get("challenge").asText();
            if (slackChallenge != null) {
              response.setChallenge(slackChallenge);
            }
            return ResponseEntity.ok(response);
          } else if (payload != null && ("shortcut".equals(slackType) || "event_callback".equals(slackType))) {
            // Handle Slack Events
            return eventAndWebhookService.processWebhook("slack", workflow, payload,
                null, null, STATUS_SUCCESS);
          } else {
            return ResponseEntity.badRequest().build();
          }
        } else {
          return ResponseEntity.badRequest().build();
        }
        
      case dockerhub:
        // TODO: dockerhub callback_url validation
        return eventAndWebhookService.processWebhook("dockerhub", workflow, payload,
            null, null, STATUS_SUCCESS);

      case generic:
        return eventAndWebhookService.processWebhook("webhook", workflow, payload,
            null, null, STATUS_SUCCESS);

      default:
        return ResponseEntity.badRequest().build();
    }
  }
//
//  /**
//   * HTTP POST Webhook specifically for the "Wait For Event" workflow task.
//   * 
//   * <h4>Sample</h4>
//   * <code>/webhook/wfe?workflowId={workflowId}&access_token={access_token}&topic={topic}&workflowActivityId={workflowActivityId}</code>
//   */
//  @PostMapping(value = "/webhook/wfe", consumes = "application/json; charset=utf-8")
//  public ResponseEntity<?> acceptWaitForEvent(HttpServletRequest request, @RequestParam String workflowId,
//      @RequestParam String workflowActivityId, @RequestParam String topic, @RequestParam(defaultValue = "success") String status,
//      @RequestBody JsonNode payload, @TokenAttribute String token) {
//      return eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "wfe", workflowId, payload,
//              workflowActivityId, topic, status);
//  }
//  
//  /**
//   * HTTP GET Webhook specifically for the "Wait For Event" workflow task.
//   * 
//   * Typically you would use the POST, however this can be useful to trigger from an email to continue or similar.
//   * 
//   * <h4>Sample</h4>
//   * <code>/webhook/wfe?workflowId={workflowId}&access_token={access_token}&topic={topic}&workflowActivityId={workflowActivityId}</code>
//   */  
//  @GetMapping(value = "/webhook/wfe")
//  public ResponseEntity<?> acceptWaitForEvent(HttpServletRequest request, @RequestParam String workflowId,
//      @RequestParam String workflowActivityId, @RequestParam String topic, @RequestParam(defaultValue = "success") String status,
//      @TokenAttribute String token) {
//      return eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "wfe", workflowId, null,
//          workflowActivityId, topic, status);
//  }
//
//  /**
//   * Accepts any JSON Cloud Event. This will map to the custom trigger but the topic will come from
//   * the CloudEvent subject.
//   * 
//   * ce attributes are in the body
//   *
//   * @see https://github.com/cloudevents/spec/blob/v1.0/json-format.md
//   * @see https://github.com/cloudevents/spec/blob/v1.0/http-protocol-binding.md
//   */
//  @PostMapping(value = "/event", consumes = "application/cloudevents+json; charset=utf-8")
//  public ResponseEntity<?> accept(@RequestBody CloudEvent event) {
//    logger.info(event.toString());
//
//    return eventService.process(event);
//  }
//  
//  /**
//   * Accepts a Cloud Event with ce attributes are in the header
//   */
//  @PostMapping("/event")
//  public ResponseEntity<?> acceptEvent(@RequestHeader HttpHeaders headers, @RequestBody String data) {
//    CloudEvent event =
//        CloudEventHttpUtils.toReader(headers, () -> data.getBytes()).toEvent();
//    logger.info(event.toString());
//
//    return eventService.process(event);
//  }
}
