package io.boomerang.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import io.boomerang.integrations.data.entity.IntegrationTemplateEntity;
import io.boomerang.integrations.model.GHLinkRequest;
import io.boomerang.integrations.service.GitHubService;
import io.boomerang.integrations.service.IntegrationService;
import io.boomerang.integrations.service.SlackService;
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
@RequestMapping("/api/v2/integration")
@Tag(name = "Integrations", description = "Handles the integrations with 3rd parties such as Slack and GitHub.")
public class IntegrationV2Controller {
    private static final Logger LOGGER = LogManager.getLogger();

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private SlackService slackService;

    @Autowired
    private GitHubService githubService;

    @GetMapping()
    @Operation(summary = "Retrieve the integrations")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request")})
    List<IntegrationTemplateEntity> get(@RequestParam Integer id) throws IOException {
      return integrationService.get();
    }
    
    /*
     * Slack Endpoints
     */
    
    /**
     * Slack Auth Endpoint
     * @param request
     * @param code
     * @return
     */
    @GetMapping(value = "/slack/auth")
    @Operation(summary = "Receive Slack Oauth2 request")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request")})
    ResponseEntity<?> receiveSlackAuth(HttpServletRequest request, @RequestParam String code) {
        return slackService.handleAuth(code);
    }

    @GetMapping(value = "/slack/install")
    @Operation(summary = "Install URL Redirect")
    @ApiResponses(value = {@ApiResponse(responseCode = "302", description = "Found")})
    ResponseEntity<?> installSlack() throws URISyntaxException {
        return slackService.installRedirect();
    }

    @PostMapping(value = "/slack/commands",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @Operation(summary = "Receive Slack Slash Commands")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request")})
    ResponseEntity<?> receiveSlackCommand(HttpServletRequest request
    // @RequestBody String body
    // @RequestHeader("x-slack-request-timestamp") String timestamp,
    // @RequestHeader("x-slack-signature") String signature,
    // @RequestParam MultiValueMap<String, String> slackEvent
    ) throws IOException {
        // LOGGER.debug("Signature: " + signature);
        // LOGGER.debug("Timestamp: " + timestamp);
        // LOGGER.debug("Payload: " + slackEvent);

        CompletableFuture.supplyAsync(slackService.createRunModal(requestValueMapper(request)));
        return ResponseEntity.ok().build();
    }

    // https://api.slack.com/reference/interaction-payloads
    @PostMapping(value = "/slack/interactivity",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
    @Operation(summary = "Receive Slack Interactivity")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request")})
    ResponseEntity<?> receiveSlackInteractivity(HttpServletRequest request
    // @RequestHeader("x-slack-request-timestamp") String timestamp,
    // @RequestHeader("x-slack-signature") String signature,
    // @RequestParam MultiValueMap<String, String> slackEvent
    ) throws JsonMappingException, JsonProcessingException {
        // LOGGER.debug(slackEvent);
        ObjectMapper mapper = new ObjectMapper();
        // JsonNode payload = mapper.readTree(slackEvent.get("payload").get(0));
        Map<String, String> slackEvent = requestValueMapper(request);
        JsonNode payload = mapper.readTree(slackEvent.get("payload"));
        if (payload.has("type") && "view_submission".equals(payload.get("type").asText())) {
            CompletableFuture.supplyAsync(slackService.executeRunModal(payload));
        } else if (payload.has("type")) {
            LOGGER.error("Unhandled Slack Interactivity Type: " + payload.get("type").asText());
        } else {
            LOGGER.error("Unhandled Slack Interactivity Payload with no Type: "
                    + payload.toPrettyString());
        }
        return ResponseEntity.ok().build();
    }

    // https://api.slack.com/apis/connections/events-api#receiving_events
    @PostMapping(value = "/slack/events", consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Receive Slack Events")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad Request")})
    ResponseEntity<?> receiveSlackEvent(HttpServletRequest request,
            @RequestHeader("x-slack-request-timestamp") String timestamp,
            @RequestHeader("x-slack-signature") String signature, @RequestBody JsonNode payload)
            throws JsonMappingException, JsonProcessingException {
        LOGGER.info(payload);
        if (payload.has("challenge")) {
            LOGGER.info("Challenge: " + payload.get("challenge"));
            return ResponseEntity.ok().body(payload.get("challenge"));
        } else if (payload.has("type")
                && "app_home_opened".equals(payload.get("event").get("type").asText())) {
            CompletableFuture.supplyAsync(slackService.appHomeOpened(payload));
        } else if (payload.has("type")
                && "app_uninstalled".equals(payload.get("event").get("type").asText())) {
            CompletableFuture.supplyAsync(slackService.appUninstalled(payload));
        } else if (payload.has("type")) {
            LOGGER.error("Unhandled Slack Event Type: " + payload.get("type").asText());
        } else {
            LOGGER.error("Unhandled Slack Event Payload with no Type: " + payload.toPrettyString());
        }
        return ResponseEntity.ok().build();
    }

    /*
     * Helper method for retrieving the URL Parameters that aren't accessible after the Spring Boot
     * filters
     *
     * Workaround reference: https://www.baeldung.com/java-url-encoding-decoding
     */
    private Map<String, String> requestValueMapper(HttpServletRequest request) {
        String body = "";
        try {
            body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        LOGGER.debug("Body: " + body);

        Map<String, String> result = Arrays.stream(body.split("&")).map(i -> i.split("="))
                .collect(Collectors.toMap(a -> a[0],
                        a -> a.length > 1 ? URLDecoder.decode(a[1], StandardCharsets.UTF_8) : ""));
        LOGGER.debug("Map: " + result.toString());
        return result;
    }
    
    /*
     * GitHub Endpoints
     */
    @GetMapping(value = "/github/installations")
    @Operation(summary = "Retrieve the installation ID and store against a team")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request")})
    ResponseEntity<?> githubInstall(@RequestParam Integer id) throws IOException {
      return githubService.retrieveAppInstallation(id);
    }

    @PostMapping(value = "/github/link")
    @Operation(summary = "Links the GitHub Installation ID with a Team")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request")})
    ResponseEntity<?> githubLink(@RequestBody GHLinkRequest request) throws IOException {
      return githubService.linkAppInstallation(request);
    }
}
