package io.boomerang.extensions;

import java.util.List;
import java.util.function.Supplier;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.client.model.ExternalTeam;

@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();

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
        final HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(https://slack.com/api/views.open, HttpMethod.GET,
            request, JsonNode.class);
        JsonNode responsePayload = response.getBody(); 
        LOGGER.info(responsePayload);
      } catch (RestClientException e) {
        LOGGER.error("Error retrievign teams");
        LOGGER.error(ExceptionUtils.getStackTrace(e));
        return false;
      }
      
      return true;
      };
  }

  private HttpHeaders buildHeaders() {

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Accept", "application/json");
    headers.add("Authorization", "Bearer " + "xoxb-1165425521975-3658671086515-LCMAzzNFbpIKxjyO04joBIwO");

    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
