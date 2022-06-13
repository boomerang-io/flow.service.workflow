package io.boomerang.extensions;

import java.io.UnsupportedEncodingException;
import java.util.function.Supplier;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.mongo.service.FlowSettingsService;

@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private FlowSettingsService flowSettingsService;

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;
  
  public Supplier<Boolean> createModal(String triggerId) {
    return () -> {
      LOGGER.info("Trigger ID:" + triggerId);
      
      String requestPayload = "{\n"
          + "  \"trigger_id\": \"" + triggerId + "\",\n"
          + "  \"view\": {\n"
          + "    \"type\": \"modal\",\n"
          + "    \"callback_id\": \"modal-identifier\",\n"
          + "    \"title\": {\n"
          + "      \"type\": \"plain_text\",\n"
          + "      \"text\": \"Just a modal\"\n"
          + "    },\n"
          + "    \"blocks\": [\n"
          + "      {\n"
          + "        \"type\": \"section\",\n"
          + "        \"block_id\": \"section-identifier\",\n"
          + "        \"text\": {\n"
          + "          \"type\": \"mrkdwn\",\n"
          + "          \"text\": \"*Welcome* to ~my~ Block Kit _modal_!\"\n"
          + "        },\n"
          + "        \"accessory\": {\n"
          + "          \"type\": \"button\",\n"
          + "          \"text\": {\n"
          + "            \"type\": \"plain_text\",\n"
          + "            \"text\": \"Just a button\"\n"
          + "          },\n"
          + "          \"action_id\": \"button-identifier\"\n"
          + "        }\n"
          + "      }\n"
          + "    ]\n"
          + "  }\n"
          + "}";
      
      try {
        final HttpHeaders headers = buildHeaders();
        final HttpEntity<String> request = new HttpEntity<String>(requestPayload.toString(), headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange("https://slack.com/api/views.open", HttpMethod.POST,
            request, JsonNode.class);
        JsonNode responsePayload = response.getBody(); 
        LOGGER.info(responsePayload);
      } catch (RestClientException e) {
        LOGGER.error("Error communicating with Slack");
        LOGGER.error(ExceptionUtils.getStackTrace(e));
        return false;
      }
      
      return true;
      };
  }

  private HttpHeaders buildHeaders() {
    
    final String authToken = flowSettingsService
        .getConfiguration("extensions", "slack.token").getValue();

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add("Authorization", "Bearer " + authToken);

    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
