package io.boomerang.v4.controller;

import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.attributes.TokenAttribute;
import io.boomerang.model.SlackEventPayload;
import io.boomerang.model.WebhookType;
import io.boomerang.service.EventService;
import io.cloudevents.CloudEvent;
import io.cloudevents.spring.http.CloudEventHttpUtils;
import io.swagger.v3.oas.annotations.tags.Tag;

/*
 * 
 */
@RestController
@RequestMapping("/api/v2")
@Tag(name = "Event and Webhook Management",
    description = "Listen for Events or Webhook requests to execute Workflows and provide the ability to resolve Wait For Event TaskRuns.")
public class EventAndWebhookV2Controller {
//
//  @PostMapping(value = "/webhook/payload", consumes = "application/json; charset=utf-8")
//  public FlowWebhookResponse submitWebhookEvent(@RequestBody RequestFlowExecution request) {
//    return webhookService.submitWebhookEvent(request);
//  }

  @Autowired
  private EventService eventService;
  
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
  public ResponseEntity<?> acceptWebhookEvent(HttpServletRequest request, @RequestParam String workflowId,
      @RequestParam WebhookType type, @RequestBody JsonNode payload, @TokenAttribute String token) {    
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
            return eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "slack", workflowId, payload,
                null, null, STATUS_SUCCESS);
          } else {
            return ResponseEntity.badRequest().build();
          }
        } else {
          return ResponseEntity.badRequest().build();
        }
        
      case dockerhub:
        // TODO: dockerhub callback_url validation
        return eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "dockerhub", workflowId, payload,
            null, null, STATUS_SUCCESS);

      case generic:
        return eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "webhook", workflowId, payload,
            null, null, STATUS_SUCCESS);

      default:
        return ResponseEntity.badRequest().build();
    }
  }
  
  /**
   * HTTP Webhook accepting Slack Slash and Interactive Commands
   * 
   * <h4>Specifications</h4>
   * <ul>
   * <li><a href="https://api.slack.com/interactivity/handling">Slack Interactivity Handling</a></li>
   * </ul>
   * 
   * <h4>Sample</h4>
   * <code>/webhook?workflowId={workflowId}&type=slack&access_token={access_token}</code>
   * @throws JsonProcessingException 
   * @throws JsonMappingException 
   */
  @PostMapping(value = "/webhook", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public ResponseEntity<?> acceptWebhookEvent(HttpServletRequest request, @RequestParam String workflowId,
      @RequestParam WebhookType type, @TokenAttribute String token, @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam MultiValueMap<String, String> slackEvent) throws JsonMappingException, JsonProcessingException {

    if (slackEvent.containsKey("payload")) {
      String encodedpayload = slackEvent.get("payload").get(0);
      String decodedPayload = encodedpayload != null ? java.net.URLDecoder.decode(encodedpayload, StandardCharsets.UTF_8) : "";

      ObjectMapper mapper = new ObjectMapper();
      JsonNode payload = mapper.readTree(decodedPayload);
      eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "slack", workflowId, payload,
          null, null, STATUS_SUCCESS);
      return ResponseEntity.ok(HttpStatus.OK);
    } else if (slackEvent.containsKey("command")) {
      
    }
    return ResponseEntity.ok(HttpStatus.UNAUTHORIZED);
  }

  /**
   * HTTP POST Webhook specifically for the "Wait For Event" workflow task.
   * 
   * <h4>Sample</h4>
   * <code>/webhook/wfe?workflowId={workflowId}&access_token={access_token}&topic={topic}&workflowActivityId={workflowActivityId}</code>
   */
  @PostMapping(value = "/webhook/wfe", consumes = "application/json; charset=utf-8")
  public ResponseEntity<?> acceptWaitForEvent(HttpServletRequest request, @RequestParam String workflowId,
      @RequestParam String workflowActivityId, @RequestParam String topic, @RequestParam(defaultValue = "success") String status,
      @RequestBody JsonNode payload, @TokenAttribute String token) {
      return eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "wfe", workflowId, payload,
              workflowActivityId, topic, status);
  }
  
  /**
   * HTTP GET Webhook specifically for the "Wait For Event" workflow task.
   * 
   * Typically you would use the POST, however this can be useful to trigger from an email to continue or similar.
   * 
   * <h4>Sample</h4>
   * <code>/webhook/wfe?workflowId={workflowId}&access_token={access_token}&topic={topic}&workflowActivityId={workflowActivityId}</code>
   */  
  @GetMapping(value = "/webhook/wfe")
  public ResponseEntity<?> acceptWaitForEvent(HttpServletRequest request, @RequestParam String workflowId,
      @RequestParam String workflowActivityId, @RequestParam String topic, @RequestParam(defaultValue = "success") String status,
      @TokenAttribute String token) {
      return eventProcessor.routeWebhookEvent(token, request.getRequestURL().toString(), "wfe", workflowId, null,
          workflowActivityId, topic, status);
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
  public ResponseEntity<?> accept(@RequestBody CloudEvent event) {
    logger.info(event.toString());

    return eventService.process(event);
  }
  
  /**
   * Accepts a Cloud Event with ce attributes are in the header
   */
  @PostMapping("/event")
  public ResponseEntity<?> acceptEvent(@RequestHeader HttpHeaders headers, @RequestBody String data) {
    CloudEvent event =
        CloudEventHttpUtils.toReader(headers, () -> data.getBytes()).toEvent();
    logger.info(event.toString());

    return eventService.process(event);
  }
}
