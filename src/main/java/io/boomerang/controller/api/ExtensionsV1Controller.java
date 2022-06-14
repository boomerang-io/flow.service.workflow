package io.boomerang.controller.api;

import java.util.concurrent.CompletableFuture;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.interceptors.AuthenticationScope;
import io.boomerang.extensions.SlackExtension;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/apis/v1/extensions")
@Tag(name = "Extensions Management",
    description = "Specific use cases via extensions.")
public class ExtensionsV1Controller {

  private static final Logger LOGGER = LogManager.getLogger();
  @Autowired
  private SlackExtension slackExtension;

  @PostMapping(value = "/slack", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @AuthenticationScope(scopes = {TokenScope.global, TokenScope.team, TokenScope.user})
  @Operation(summary = "Receive Slack commands")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  ResponseEntity<?> receiveSlackCommand(HttpServletRequest request, 
      @RequestHeader("x-slack-request-timestamp") String timestamp,
      @RequestHeader("x-slack-signature") String signature,
      @RequestParam MultiValueMap<String, String> slackEvent) throws JsonMappingException, JsonProcessingException {
    LOGGER.info(slackEvent);
    CompletableFuture.supplyAsync(() -> slackExtension.createModal(slackEvent.get("trigger_id").get(0)));
    return ResponseEntity.ok().build();
  }
}
