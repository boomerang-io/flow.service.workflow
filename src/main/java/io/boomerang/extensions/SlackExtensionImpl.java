package io.boomerang.extensions;

import java.io.IOException;
import java.util.LinkedList;
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
import com.slack.api.model.view.View.ViewBuilder;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowWorkflowService;

@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private FlowSettingsService flowSettingsService;

  @Autowired
  private FlowWorkflowService workflowRepository;

  public Supplier<Boolean> createInitialRunModal(String triggerId, String userId, String workflowId) {
    return () -> {
      LOGGER.info("Trigger ID:" + triggerId);

      final String authToken =
          flowSettingsService.getConfiguration("extensions", "slack.token").getValue();

      Slack slack = Slack.getInstance();
      
      ViewBuilder modalViewBuilder = View.builder().type("modal")
          .title(ViewTitle.builder().type("plain_text").text("Run Workflow").emoji(true).build())
          .callbackId("Workflow Run Modal");
      final WorkflowEntity workflowSummary = workflowRepository.getWorkflow(workflowId);

      SectionBlock detailBlock;
      if (workflowSummary != null) {
        final String workflowName = workflowSummary.getName();
        LOGGER.info("Workflow Name: " + workflowName);
        
        List<TextObject> workflowFields = new LinkedList<>();
        workflowFields.add(MarkdownTextObject.builder()
            .text("- *ID:* " + workflowId)
            .build());
        workflowFields.add(MarkdownTextObject.builder()
            .text("- *Name:* " + workflowName)
            .build());

        detailBlock = SectionBlock.builder().blockId("workflow_fields").text(MarkdownTextObject.builder().text("*Workflow Details*")
            .build()).fields(workflowFields).build();
        modalViewBuilder.submit(ViewSubmit.builder().type("plain_text").text("Run").build());
      } else {
        LOGGER.info("Unable to find Workflow with ID: " + workflowId);
        detailBlock = SectionBlock.builder().blockId("workflow_fields").text(MarkdownTextObject.builder().text(":slightly_frowning_face: Unfortunately we are unable to find a Workflow with the specified ID (" + workflowId + ")")
            .build()).build();
      }
      modalViewBuilder.blocks(Blocks.asBlocks(
          SectionBlock.builder().blockId("workflow_title")
              .text(MarkdownTextObject.builder().text("_Welcome to the better way to automate with no-code workflow automation._")
                  .build())
              .build(),
              detailBlock)          )
          .privateMetadata(workflowId)
          .close(ViewClose.builder().type("plain_text").text("Close").build());
      View modalView = modalViewBuilder.build();

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

//  public Supplier<Boolean> updateRunModal(String triggerId, String userId, String workflowId) {
//    return () -> {
//      LOGGER.info("Trigger ID:" + triggerId);
//
//      final String authToken =
//          flowSettingsService.getConfiguration("extensions", "slack.token").getValue();
//
//      Slack slack = Slack.getInstance();
//      
//      UsersReadResponse user;
//      try {
//        user = slack.scim(authToken).readUser(UsersReadRequest.builder().id(userId).build());
//        LOGGER.info(user.toString());
//      } catch (IOException | SCIMApiException e1) {
//        LOGGER.error(e1.toString());
//        return false;
//      }
//
//      List<TextObject> workflowFields = new LinkedList<>();
//
//      final WorkflowEntity workflowSummary = workflowRepository.getWorkflow(workflowId);
//
//      if (workflowSummary != null) {
//        final String workflowName = workflowSummary.getName();
//        LOGGER.info("Workflow Name: " + workflowName);
//        workflowFields.add(MarkdownTextObject.builder()
//            .text("Workflow to be triggered;\\n - ID: " + workflowId + "\n - Name: " + workflowName)
//            .build());
//      } else {
//        LOGGER.info("Unable to find Workflow with ID: " + workflowId);
//        workflowFields.add(MarkdownTextObject.builder()
//            .text("Unable to find Workflow with ID: " + workflowId).build());
//      }
//
//      View modalView = View.builder().type("modal")
//          .title(ViewTitle.builder().type("plain_text").text("Run Workflow").emoji(true).build())
//          .callbackId("Workflow Run Modal")
//          .blocks(Blocks.asBlocks(
//              SectionBlock.builder().blockId("workflow_title")
//                  .text(MarkdownTextObject.builder().text("*Welcome* to a better way to automate.")
//                      .build())
//                  .build(),
//              SectionBlock.builder().blockId("workflow_fields").fields(workflowFields).build()))
//          .submit(ViewSubmit.builder().type("plain_text").text("Submit").build())
//          .close(ViewClose.builder().type("plain_text").text("Close").build()).build();
//
//      ViewsOpenResponse viewResponse;
//      try {
//        viewResponse =
//            slack.methods(authToken).viewsOpen(req -> req.triggerId(triggerId).view(modalView));
//        LOGGER.info(viewResponse.toString());
//      } catch (IOException | SlackApiException e) {
//        LOGGER.error(e.toString());
//        return false;
//      }
//
//      return true;
//    };
//  }
}
