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
  
  public Supplier<Boolean> createModal(String triggerId) {
    return () -> {
      LOGGER.info("Trigger ID:" + triggerId);
      System.out.println("Trigger ID:" + triggerId);
      
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
        e.printStackTrace();
        return false;
      }
      
      return true;
      };
  }
}
