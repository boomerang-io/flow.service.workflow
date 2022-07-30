package io.boomerang.controller.api;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import io.boomerang.extensions.SlackExtension;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/*
 * The extensions management controller
 * 
 * The Slack extension depends on the SlackSecurityVerificationFilter
 */
@RestController
@RequestMapping("/apis/v1/extensions")
@Tag(name = "Extensions Management",
    description = "Specific use cases via extensions.")
public class ExtensionsV1Controller {

  private static final Logger LOGGER = LogManager.getLogger();
  @Autowired
  private SlackExtension slackExtension;
  
  @GetMapping(value = "/slack/auth")
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Receive Slack Oauth2 request")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackAuth(HttpServletRequest request,
      @RequestParam String code) {
    return slackExtension.handleAuth(code);
  }
  
  @GetMapping(value = "/slack/install")
  @Operation(summary = "Install URL Redirect")
  @ApiResponses(value = {@ApiResponse(responseCode = "302", description = "Found")})
  ResponseEntity<?> installSlack() throws URISyntaxException {
    return slackExtension.installRedirect();
  }

  @PostMapping(value = "/slack/commands", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Receive Slack Slash Commands")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackCommand(HttpServletRequest request,
      // @RequestBody String body
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam MultiValueMap<String, String> slackEvent) throws IOException {
    String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    LOGGER.debug("Body: " + body);
    LOGGER.debug("Signature: " + signature);
    LOGGER.debug("Timestamp: " + timestamp);
    LOGGER.debug("Payload: " + slackEvent);
    
    //Workaround reference: https://www.baeldung.com/java-url-encoding-decoding
    Map<String, String> result = Arrays.stream(body.split("&"))
        .map(i -> i.split("="))
        .collect(Collectors.toMap(
            a -> a[0],
            a -> URLDecoder.decode(a[1], StandardCharsets.UTF_8)));
    
    LOGGER.debug("Map: " + result.toString());

    CompletableFuture.supplyAsync(slackExtension.createRunModal(result));
    return ResponseEntity.ok().build();
  }
  
  //https://api.slack.com/reference/interaction-payloads
  @PostMapping(value = "/slack/interactivity", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Receive Slack Interactivity")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackInteractivity(HttpServletRequest request,
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam MultiValueMap<String, String> slackEvent) throws JsonMappingException, JsonProcessingException {
    LOGGER.debug(slackEvent);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode payload = mapper.readTree(slackEvent.get("payload").get(0));
    if (payload.has("type") && "view_submission".equals(payload.get("type").asText())) {
      CompletableFuture.supplyAsync(slackExtension.executeRunModal(payload));
    } else if (payload.has("type")) {
      LOGGER.error("Unhandled Slack Interactivity Type: " + payload.get("type").asText());
    } else {
      LOGGER.error("Unhandled Slack Interactivity Payload with no Type: " + payload.toPrettyString());
    }
    return ResponseEntity.ok().build();
  }
  
  //https://api.slack.com/apis/connections/events-api#receiving_events
  @PostMapping(value = "/slack/events", consumes = {MediaType.APPLICATION_JSON_VALUE})
  @AuthenticationScope(scopes = {TokenScope.global})
  @Operation(summary = "Receive Slack Events")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackEvent(HttpServletRequest request, 
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestBody JsonNode payload) throws JsonMappingException, JsonProcessingException {
    LOGGER.info(payload);
    if (payload.has("challenge")) {
      LOGGER.info("Challenge: " + payload.get("challenge"));
      return ResponseEntity.ok().body(payload.get("challenge"));
    } else if (payload.has("type") && "app_home_opened".equals(payload.get("event").get("type").asText())) {
      CompletableFuture.supplyAsync(slackExtension.appHomeOpened(payload));
    } else if (payload.has("type")) {
      LOGGER.error("Unhandled Slack Event Type: " + payload.get("type").asText());
    } else {
      LOGGER.error("Unhandled Slack Event Payload with no Type: " + payload.toPrettyString());
    }
    return ResponseEntity.ok().build();
  }
}
