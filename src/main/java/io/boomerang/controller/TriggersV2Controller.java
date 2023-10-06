package io.boomerang.controller;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.SlackEventPayload;
import io.boomerang.model.WebhookType;
import io.boomerang.service.TriggerService;
import io.cloudevents.CloudEvent;
import io.cloudevents.spring.http.CloudEventHttpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/triggers")
@Tag(name = "Triggers for Events, Topics, and Webhooks",
    description = "Listen for Events or Webhook requests to trigger Workflows and provide the ability to resolve Wait For Event TaskRuns.")
public class TriggersV2Controller {

  @Autowired
  private TriggerService listenerService;
  
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
   * <p>Can use Authorization header or access_token URL Parameter</p>
   * 
   * <h4>Sample</h4>
   * <code>/webhook?workflow={workflow}&type={generic|slack|dockerhub}&access_token={access_token}</code>
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
            return listenerService.processWebhook("slack", workflow, payload);
          } else {
            return ResponseEntity.badRequest().build();
          }
        } else {
          return ResponseEntity.badRequest().build();
        }
      case dockerhub:
        // TODO: dockerhub callback_url validation
        return listenerService.processWebhook("dockerhub", workflow, payload);
      case generic:
        return listenerService.processWebhook("webhook", workflow, payload);
      case github:
        //TODO build out the GitHub Webhook receiver
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
      default:
        return ResponseEntity.badRequest().build();
    }
  }

  /**
   * HTTP POST specifically for the "Wait For Event" workflow task.
   * 
   * <h4>Sample</h4>
   * <code>/topic?workflow={workflow}&workflowrun={workflowrun}&topic={topic}&status={status}&access_token={access_token}</code>
   */
  @PostMapping(value = "/topic", consumes = "application/json; charset=utf-8")
  public ResponseEntity<?> acceptWaitForEvent(
      @Parameter(name = "workflow",
      description = "The Workflow the request relates to",
      required = true) @RequestParam(required = true) String workflow,
      @Parameter(name = "workflowrun",
      description = "The WorkflowRun the request relates to",
      required = true) @RequestParam(required = true) String workflowrun,
      @Parameter(name = "topic",
      description = "The topic to publish to",
      required = true) @RequestParam(required = true) String topic,
      @Parameter(name = "status",
      description = "The status to set the wait for end to",
      required = false) @RequestParam(defaultValue = "success") String status,
      @RequestBody JsonNode payload) {
      return listenerService.processWFE(workflow, workflowrun, topic, status, Optional.of(payload));
  }
  
  /**
   * HTTP GET specifically for the "Wait For Event" workflow task.
   * 
   * Typically you would use the POST, however this can be useful to trigger from an email to continue or similar.
   * 
   * <h4>Sample</h4>
   * <code>/topic?workflow={workflow}&workflowrun={workflowrun}&topic={topic}&status={status}&access_token={access_token}</code>
   */  
  @GetMapping(value = "/topic")
  public ResponseEntity<?> acceptWaitForEvent(@Parameter(name = "workflow",
      description = "The Workflow the request relates to",
      required = true) @RequestParam(required = true) String workflow,
      @Parameter(name = "workflowrun",
      description = "The WorkflowRun the request relates to",
      required = true) @RequestParam(required = true) String workflowrun,
      @Parameter(name = "topic",
      description = "The topic to publish to",
      required = true) @RequestParam(required = true) String topic,
      @Parameter(name = "status",
      description = "The status to set for the WaitForEvent TaskRun",
      required = false) @RequestParam(defaultValue = "success") String status) {
        return listenerService.processWFE(workflow, workflowrun, topic, status, Optional.empty());
  }

  /**
   * Accepts any JSON Cloud Event. This will map to the custom trigger but the topic will come from
   * the CloudEvent subject.
   * 
   * ce attributes are in the body
   *
   * @see https://github.com/cloudevents/spec/blob/v1.0/json-format.md
   * @see https://github.com/cloudevents/spec/blob/v1.0/http-protocol-binding.md
   */
  @PostMapping(value = "/event", consumes = "application/cloudevents+json; charset=utf-8")
  public ResponseEntity<?> accept(@Parameter(name = "workflow",
      description = "The Workflow the request relates to",
      required = false) @RequestParam(required = false) Optional<String> workflow,
      @RequestBody CloudEvent event) {
    return listenerService.processEvent(event, workflow);
  }
  
  /**
   * Accepts a Cloud Event with ce attributes are in the header
   */
  @PostMapping("/event")
  public ResponseEntity<?> acceptEvent(@Parameter(name = "workflow",
      description = "The Workflow the request relates to",
      required = false) @RequestParam(required = false) Optional<String> workflow,
      @RequestHeader HttpHeaders headers, 
      @RequestBody String data) {
    CloudEvent event =
        CloudEventHttpUtils.toReader(headers, () -> data.getBytes()).toEvent();
    return listenerService.processEvent(event, workflow);
  }
}
