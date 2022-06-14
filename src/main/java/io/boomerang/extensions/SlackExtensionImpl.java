package io.boomerang.extensions;

import java.io.IOException;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.Blocks;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewTitle;
import io.boomerang.mongo.service.FlowSettingsService;

@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private FlowSettingsService flowSettingsService;

//  @Autowired
//  @Qualifier("internalRestTemplate")
//  private RestTemplate restTemplate;
  
  public Supplier<Boolean> createModal(String triggerId) {
    return () -> {
      LOGGER.info("Trigger ID:" + triggerId);
      
      final String authToken = flowSettingsService
          .getConfiguration("extensions", "slack.token").getValue();
      
      Slack slack = Slack.getInstance();
      
      View modalView = View.builder()
          .type("modal")
          .title(ViewTitle.builder().type("plain_text").text("Workflow Modal").emoji(true).build())
          .callbackId("Workflow Run Modal")
          .blocks(Blocks.asBlocks(SectionBlock.builder().blockId("Workflow").text(MarkdownTextObject.builder().text("*Welcome* to ~my~ Block Kit _modal_!").build()).build()))
          .build();
      
      ViewsOpenResponse viewResponse;
      try {
        viewResponse = slack.methods(authToken).viewsOpen(req -> req.triggerId(triggerId).view(modalView));

        LOGGER.info(viewResponse.toString());
      } catch (IOException | SlackApiException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return false;
      }
//      try {
//        final HttpHeaders headers = buildHeaders();
//        final HttpEntity<String> request = new HttpEntity<String>(requestPayload, headers);
//        
//        LOGGER.info(request.toString());
//
//        ResponseEntity<JsonNode> response = restTemplate.exchange("https://slack.com/api/views.open", HttpMethod.POST,
//            request, JsonNode.class);
//        JsonNode responsePayload = response.getBody(); 
//        LOGGER.info(responsePayload);
//      } catch (RestClientException e) {
//        LOGGER.error("Error communicating with Slack");
//        LOGGER.error(ExceptionUtils.getStackTrace(e));
//        return false;
//      }
      
      return true;
      };
  }

//  private HttpHeaders buildHeaders() {
//    
//    final String authToken = flowSettingsService
//        .getConfiguration("extensions", "slack.token").getValue();
//    
//    LOGGER.info("Slack Token:" + authToken);
//
//    final HttpHeaders headers = new HttpHeaders();
//    headers.add("Accept", "application/json");
//    headers.add("Authorization", "Bearer " + authToken);
//
//    headers.setContentType(MediaType.APPLICATION_JSON);
//    return headers;
//  }
}
