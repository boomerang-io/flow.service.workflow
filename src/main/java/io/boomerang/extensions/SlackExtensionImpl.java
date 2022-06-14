package io.boomerang.extensions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import com.slack.api.model.block.composition.TextObject;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.service.crud.WorkflowService;

@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private FlowSettingsService flowSettingsService;

  @Autowired
  private WorkflowService workflowService;

  public Supplier<Boolean> createInitialModal(String triggerId, String workflowId) {
    return () -> {
      LOGGER.info("Trigger ID:" + triggerId);

      final String authToken =
          flowSettingsService.getConfiguration("extensions", "slack.token").getValue();

      Slack slack = Slack.getInstance();

      List<TextObject> workflowFields = new ArrayList<>();

      final WorkflowSummary workflowSummary = workflowService.getWorkflow(workflowId);

      if (workflowSummary != null) {
        final String workflowName = workflowService.getWorkflow(workflowId).getName();
        workflowFields.add(MarkdownTextObject.builder()
            .text("Workflow to be triggered;\\n - ID: " + workflowId + "\n - Name: " + workflowName)
            .build());
      } else {
        workflowFields.add(MarkdownTextObject.builder()
            .text("Unable to find Workflow with ID: " + workflowId).build());
      }

      View modalView = View.builder().type("modal")
          .title(ViewTitle.builder().type("plain_text").text("Run Workflow").emoji(true).build())
          .callbackId("Workflow Run Modal")
          .blocks(Blocks.asBlocks(
              SectionBlock.builder().blockId("workflow_title")
                  .text(MarkdownTextObject.builder().text("*Welcome* to a better way to automate.")
                      .build())
                  .build(),
              SectionBlock.builder().blockId("workflow_fields").fields(workflowFields).build()))
          .submit(ViewSubmit.builder().type("plain_text").text("Submit").build())
          .close(ViewClose.builder().type("plain_text").text("Close").build()).build();

      ViewsOpenResponse viewResponse;
      try {
        viewResponse =
            slack.methods(authToken).viewsOpen(req -> req.triggerId(triggerId).view(modalView));
        LOGGER.info(viewResponse.toString());
      } catch (IOException | SlackApiException e) {
        LOGGER.error(e.toString());
        return false;
      }

      return true;
    };
  }
}
