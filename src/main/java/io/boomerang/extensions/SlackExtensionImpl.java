package io.boomerang.extensions;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.view.View;
import com.slack.api.model.view.View.ViewBuilder;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import io.boomerang.model.FlowActivity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.service.ExecutionService;

@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private FlowSettingsService flowSettingsService;

  @Autowired
  private FlowWorkflowService workflowRepository;

  @Autowired
  private ExecutionService executionService;

  @Value("${flow.apps.flow.url}")
  private String flowAppsUrl;
  
  private static final String MODAL_TEXT_TAGLINE = "The better way to automate with no-code workflow automation.";

  public Supplier<Boolean> createRunModal(String triggerId, String userId, String workflowId) {
    return () -> {
      LOGGER.info("Trigger ID:" + triggerId);

      final String authToken =
          flowSettingsService.getConfiguration("extensions", "slack.token").getValue();

      ViewBuilder modalViewBuilder = View.builder().type("modal")
          .title(ViewTitle.builder().type("plain_text").text("Run Workflow").emoji(true).build())
          .callbackId("workflow-run-modal").privateMetadata(workflowId)
          .close(ViewClose.builder().type("plain_text").text("Close").build());

      List<LayoutBlock> blocks = new LinkedList<>();
      SectionBlock titleBlock = SectionBlock.builder().blockId("workflow_title")
          .text(MarkdownTextObject.builder()
              .text("_" + MODAL_TEXT_TAGLINE + "_")
              .build())
          .build();
      blocks.add(titleBlock);

      final WorkflowEntity workflowSummary = workflowRepository.getWorkflow(workflowId);
      if (workflowSummary != null) {
        LOGGER.info("Workflow Name: " + workflowSummary.getName());
        HeaderBlock headerBlock = HeaderBlock.builder()
            .text(PlainTextObject.builder().text("Workflow Details").build()).build();
        blocks.add(headerBlock);
        SectionBlock detailBlock =
            SectionBlock.builder()
                .text(
                    MarkdownTextObject.builder()
                        .text("*ID:* " + workflowId + "\n*Name:* " + workflowSummary.getName()
                            + "\n*Summary:* " + workflowSummary.getShortDescription())
                        .build())
                .build();
        blocks.add(detailBlock);
        modalViewBuilder.submit(ViewSubmit.builder().type("plain_text").text(":point_right: Run it")
            .emoji(true).build());
      } else {
        LOGGER.info("Unable to find Workflow with ID: " + workflowId);
        SectionBlock errorBlock = SectionBlock.builder().blockId("workflow_fields")
            .text(MarkdownTextObject.builder().text(
                ":slightly_frowning_face: Unfortunately we are unable to find a Workflow with the specified ID ("
                    + workflowId + ")")
                .build())
            .build();
        blocks.add(errorBlock);
      }
      DividerBlock dividerBlock = DividerBlock.builder().build();
      blocks.add(dividerBlock);
      List<ContextBlockElement> elementsList = new LinkedList<>();
      elementsList.add(MarkdownTextObject.builder().text(
          ":bulb: This integration is in _alpha_ and currently only works with Workflows that do not require parameters to be entered.")
          .build());
      ContextBlock contextBlock = ContextBlock.builder().elements(elementsList).build();
      blocks.add(contextBlock);
      modalViewBuilder.blocks(blocks);
      View modalView = modalViewBuilder.build();

      Slack slack = Slack.getInstance();
      try {
        ViewsOpenResponse viewResponse =
            slack.methods(authToken).viewsOpen(req -> req.triggerId(triggerId).view(modalView));
        LOGGER.info(viewResponse.toString());
      } catch (IOException | SlackApiException e) {
        LOGGER.error(e.toString());
        return false;
      }

      return true;
    };
  }

//  public Supplier<Boolean> executeRunModal(JsonNode jsonPayload) {
//    return () -> {
  public SlackResponseActionModel executeRunModal(JsonNode jsonPayload) {
      final String userId = jsonPayload.get("user").get("id").asText();
      LOGGER.info("User ID: " + userId);
      final String triggerId = jsonPayload.get("trigger_id").asText();
      LOGGER.info("Trigger ID: " + triggerId);
      final String workflowId = jsonPayload.get("view").get("private_metadata").asText();
      LOGGER.info("Workflow ID: " + workflowId);
      final String rootViewId = jsonPayload.get("view").get("root_view_id").asText();
      LOGGER.info("Root View ID: " + rootViewId);
      final String authToken =
          flowSettingsService.getConfiguration("extensions", "slack.token").getValue();

      Slack slack = Slack.getInstance();

      try {
        UsersInfoResponse userInfo =
            slack.methods(authToken).usersInfo(UsersInfoRequest.builder().user(userId).build());
        LOGGER.info("User Info: " + userInfo.toString());
      } catch (IOException | SlackApiException e2) {
        LOGGER.error(e2);
        // TODO: return error modal
      }

      // TODO check if user has rights to trigger workflow
      FlowActivity flowActivity = executionService.executeWorkflow(workflowId, Optional.of("webhook"),
          Optional.empty(), Optional.empty());

      List<LayoutBlock> blocks = new LinkedList<>();

      LOGGER.info("Workflow Name: " + flowActivity.getWorkflowName());
      HeaderBlock headerBlock = HeaderBlock.builder()
          .text(PlainTextObject.builder().text("Workflow Details").build()).build();
      blocks.add(headerBlock);
      SectionBlock detailBlock = SectionBlock.builder()
          .text(MarkdownTextObject.builder()
              .text("*ID:* " + workflowId + "\n*Name:* " + flowActivity.getWorkflowName()
                  + "\n*Summary:* " + flowActivity.getShortDescription())
              .build())
          .build();
      blocks.add(detailBlock);
      HeaderBlock activityHeaderBlock = HeaderBlock.builder()
          .text(PlainTextObject.builder().text("Workflow Activity").build()).build();
      blocks.add(activityHeaderBlock);
      SectionBlock activityDetailBlock = SectionBlock.builder()
          .text(MarkdownTextObject.builder().text("<" + flowAppsUrl + "/activity/" + workflowId
              + "/execution/" + flowActivity.getId() + "|View your workflow activity>.").build())
          .build();
      blocks.add(activityDetailBlock);
      
      SlackResponseActionModel response = new SlackResponseActionModel("update", updateView(workflowId, blocks));
      
      return response;

//      try {
//        ViewsUpdateResponse viewResponse = slack.methods(authToken)
//            .viewsUpdate(req -> req.viewId(rootViewId).view(updateView(workflowId, blocks)));
//        LOGGER.info(viewResponse.toString());
//      } catch (IOException | SlackApiException e) {
//        LOGGER.error(e.toString());
//        return false;
//      }
//      return true;
//    };
  }

  private View updateView(String workflowId, List<LayoutBlock> middleBlocks) {
    List<LayoutBlock> topBlocks = new LinkedList<>();
    SectionBlock titleBlock = SectionBlock.builder().blockId("workflow_title")
        .text(MarkdownTextObject.builder()
            .text("_" + MODAL_TEXT_TAGLINE + "_")
            .build())
        .build();
    topBlocks.add(titleBlock);
    List<LayoutBlock> bottomBlocks = new LinkedList<>();
    DividerBlock dividerBlock = DividerBlock.builder().build();
    bottomBlocks.add(dividerBlock);
    List<ContextBlockElement> elementsList = new LinkedList<>();
    elementsList.add(MarkdownTextObject.builder().text(
        ":bulb: This integration is in _alpha_ and currently only works with Workflows that do not require parameters to be entered.")
        .build());
    ContextBlock contextBlock = ContextBlock.builder().elements(elementsList).build();
    bottomBlocks.add(contextBlock);
    topBlocks.addAll(middleBlocks);
    topBlocks.addAll(bottomBlocks);
    return View.builder().type("modal")
        .title(ViewTitle.builder().type("plain_text").text("Run Workflow").emoji(true).build())
        .callbackId("workflow-run-modal").privateMetadata(workflowId).blocks(topBlocks)
        .close(ViewClose.builder().type("plain_text").text("Close").build()).build();

  }
}
