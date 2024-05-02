package io.boomerang.controller;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import io.boomerang.service.TriggerService;
import io.cloudevents.CloudEvent;
import io.cloudevents.spring.http.CloudEventHttpUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2")
@Tag(name = "Triggers for Events, Topics, and Webhooks",
    description = "Listen for Events or Webhook requests to trigger Workflows and provide the ability to resolve Wait For Event TaskRuns.")
public class TriggerV2Controller {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private TriggerService triggerService;

  /**
   * HTTP Webhook accepting Generic, Slack Events, and Dockerhub subtypes. For Slack and Dockerhub
   * will respond/perform verification challenges.
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
   * <p>
   * Can use Authorization header or access_token URL Parameter
   * </p>
   * 
   * <h4>Sample</h4>
   * <code>/webhook?workflow={workflow}&access_token={access_token}</code>
   */
  @PostMapping(value = "/webhook", consumes = "application/json; charset=utf-8")
  @Operation(summary = "Trigger WorkflowRun via Webhook.")
  public ResponseEntity<?> acceptWebhookEvent(
      @Parameter(name = "workflow", description = "Workflow reference the request relates to",
          required = false) @RequestParam(required = false) Optional<String> workflow,
      @RequestBody JsonNode payload,
      HttpServletRequest request) {
    request.getHeaderNames().asIterator()
    .forEachRemaining(headerName ->
            LOGGER.debug("HEADER::" + headerName + ": " + request.getHeader(headerName)));
    if (request.getHeader("x-slack-signature")!= null) {
      if (payload != null) {
        final String slackType = payload.get("type").asText();

        if ("url_verification".equals(slackType)) {
          SlackEventPayload response = new SlackEventPayload();
          final String slackChallenge = payload.get("challenge").asText();
          if (slackChallenge != null) {
            response.setChallenge(slackChallenge);
          }
          return ResponseEntity.ok(response);
        } else if (payload != null
            && ("shortcut".equals(slackType) || "event_callback".equals(slackType))) {
          // Handle Slack Events
          // TODO change this to processSlackWebhook
          return ResponseEntity.ok(triggerService.processWebhook(workflow.get(), payload));
        } else {
          return ResponseEntity.badRequest().build();
        }
      } else {
        return ResponseEntity.badRequest().build();
      }
    } else if (request.getHeader("x-github-event")!= null) {
      String ghEventType = request.getHeader("x-github-event");
      if (ghEventType != null) {
        triggerService.processGitHubWebhook("github", ghEventType, payload);    
        return ResponseEntity.ok().build();
      }
      return ResponseEntity.badRequest().build();
    }    
    return ResponseEntity.ok(triggerService.processWebhook(workflow.get(), payload));
  }

  /**
   * HTTP POST specifically for the "Wait For Event" workflow task.
   * 
   * <h4>Sample</h4>
   * <code>/callback?id={workflowrun}&topic={topic}&status={status}&access_token={access_token}</code>
   */
  @PostMapping(value = "/callback", consumes = "application/json; charset=utf-8")
  public void acceptWaitForEvent(
      @Parameter(name = "workflowrun", description = "The WorkflowRun the request relates to",
          required = true) @RequestParam(required = true) String workflowrun,
      @Parameter(name = "topic", description = "The topic to publish to",
          required = true) @RequestParam(required = true) String topic,
      @Parameter(name = "status", description = "The status to set for the WaitForEvent TaskRun. Succeeded | Failed.",
          required = false) @RequestParam(defaultValue = "succeeded") String status,
      @RequestBody JsonNode payload) {
    triggerService.processWFE(workflowrun, topic, status, Optional.of(payload));
  }

  /**
   * HTTP GET specifically for the "Wait For Event" workflow task.
   * 
   * Typically you would use the POST, however this can be useful to trigger from an email to
   * continue or similar.
   * 
   * <h4>Sample</h4>
   * <code>/callback?id={workflowrun}&topic={topic}&status={status}&access_token={access_token}</code>
   */
  @GetMapping(value = "/callback")
  public void acceptWaitForEvent(
      @Parameter(name = "workflowrun", description = "The WorkflowRun the request relates to",
          required = true) @RequestParam(required = true) String workflowrun,
      @Parameter(name = "topic", description = "The topic to publish to",
          required = true) @RequestParam(required = true) String topic,
      @Parameter(name = "status", description = "The status to set for the WaitForEvent TaskRun. Succeeded | Failed.",
          required = false) @RequestParam(defaultValue = "succeeded") String status) {
    triggerService.processWFE(workflowrun, topic, status, Optional.empty());
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
  public ResponseEntity<?> accept(
      @Parameter(name = "workflow", description = "The Workflow the request relates to",
          required = false) @RequestParam(required = false) Optional<String> workflow,
      @RequestBody CloudEvent event) {
    return ResponseEntity.ok(triggerService.processEvent(event, workflow));
  }

  /**
   * Accepts a Cloud Event with ce attributes are in the header
   */
  @PostMapping("/event")
  public ResponseEntity<?> acceptEvent(
      @Parameter(name = "workflow", description = "The Workflow the request relates to",
          required = false) @RequestParam(required = false) Optional<String> workflow,
      @RequestHeader HttpHeaders headers, @RequestBody String data) {
    CloudEvent event = CloudEventHttpUtils.toReader(headers, () -> data.getBytes()).toEvent();
    return ResponseEntity.ok(triggerService.processEvent(event, workflow));
  }
}
