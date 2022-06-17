package io.boomerang.extensions;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.views.ViewsOpenResponse;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.ContextBlock;
import com.slack.api.model.block.ContextBlockElement;
import com.slack.api.model.block.DividerBlock;
import com.slack.api.model.block.HeaderBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.BlockElement;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.View.ViewBuilder;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import io.boomerang.error.BoomerangException;
import io.boomerang.exceptions.RunWorkflowException;
import io.boomerang.model.FlowActivity;
import io.boomerang.mongo.entity.ExtensionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.repository.ExtensionRepository;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.service.ExecutionService;
import io.boomerang.service.FilterService;
import io.boomerang.util.ParameterMapper;

/*
 * Handles the Slack app slash command and interactivity interactions
 * 
 * The Slack app needs the following oauth scopes:
 * - chat:write
 * - commands
 * - users:read
 * - users:read:email
 */
@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private FlowSettingsService flowSettingsService;

  @Autowired
  private FlowWorkflowService workflowRepository;
  
  @Autowired
  private ExecutionService executionService;

  @Autowired
  private ExtensionRepository extensionsRepository;

  @Autowired
  private FilterService filterService;

  @Value("${flow.apps.flow.url}")
  private String flowAppsUrl;

  private static final String MODAL_TEXT_TAGLINE =
      "The better way to automate with no-code workflow automation.";
  
  private static final String MODAL_TEXT_FOOTER =
  ":bulb: This integration is in _alpha_ and currently only works with Workflows that do not require parameters to be entered. You also need to be a registered user with access to the Workflow.";

  private static final String EXTENSION_TYPE =
      "slack_auth";

  /*
   * Processes a Slash command and generates a Modal for the end user to confirm Workflow Run details
   * 
   * <h4>Specifications</h4>
   * <ul>
   * <li><a href="https://api.slack.com/interactivity/slash-commands">Slack Slash Commands</a></li>
   * <li><a href="https://api.slack.com/surfaces/modals/using">Slack Modals</a></li>
   * </ul>
   * 
   * @param triggerId     the Slack Trigger ID
   * @param userId        the Slack User ID
   * @param workflowId    the user entered workflow ID
   * @return Supplier       To be used by the CompletableFuture
   */
  public Supplier<Boolean> createRunModal(String triggerId, String userId, String teamId, String workflowId) {
    return () -> {
      final String authToken = getSlackAuthToken(teamId);

      ViewBuilder modalViewBuilder = View.builder().type("modal")
          .title(ViewTitle.builder().type("plain_text").text("Run Workflow").emoji(true).build())
          .callbackId("workflow-run-modal").privateMetadata(workflowId)
          .close(ViewClose.builder().type("plain_text").text("Close").build());

      Slack slack = Slack.getInstance();
      Boolean notFound = false;
      WorkflowEntity workflowSummary = new WorkflowEntity();
      try {
        UsersInfoResponse userInfo =
            slack.methods(authToken).usersInfo(UsersInfoRequest.builder().user(userId).build());
        LOGGER.debug("User Info: " + userInfo.toString());
        if (userInfo != null && userInfo.getUser() != null
            && userInfo.getUser().getProfile() != null) {
          // Trigger workflow Execution and impersonate Slack user
          String userEmail = userInfo.getUser().getProfile().getEmail();
          if (userEmail != null && canExecuteWorkflow(workflowId, userEmail)) {
            workflowSummary = workflowRepository.getWorkflow(workflowId);
            if (workflowSummary != null) {
              modalViewBuilder.submit(ViewSubmit.builder().type("plain_text")
                  .text(":point_right: Run it").emoji(true).build());
            } else {
              LOGGER.debug("Unable to find Workflow with specified ID (" + workflowId + ")");
              notFound = true;
            }
          } else {
            notFound = true;
          }
        }
      } catch (IOException | SlackApiException e) {
        LOGGER.error(e.toString());
        return false;
      }

      View modalView = modalViewBuilder.blocks(modalRunBlocks(workflowId, workflowSummary.getName(),
          workflowSummary.getShortDescription(), notFound)).build();
      try {
        ViewsOpenResponse viewResponse =
            slack.methods(authToken).viewsOpen(req -> req.triggerId(triggerId).view(modalView));
        LOGGER.debug(viewResponse.toString());
      } catch (IOException | SlackApiException e) {
        LOGGER.error(e.toString());
        return false;
      }

      return true;
    };
  }

  /*
   * Creates the Blocks that are part of the Run Modal response
   */
  private List<LayoutBlock> modalRunBlocks(String workflowId, String workflowName,
      String workflowSummary, Boolean notFound) {
    List<LayoutBlock> blocks = new LinkedList<>();
    blocks.add(SectionBlock.builder().blockId("workflow_title")
          .text(MarkdownTextObject.builder().text("_" + MODAL_TEXT_TAGLINE + "_").build()).build());
    blocks.add(HeaderBlock.builder()
        .text(PlainTextObject.builder().text("Workflow Details").build()).build());
    if (!notFound) {
    blocks.add(SectionBlock.builder()
        .text(PlainTextObject.builder()
            .text("Confirm the following details and click 'Run It' to trigger the workflow. A message will be sent to you with the activity details.").build())
        .build());
    blocks
        .add(
            SectionBlock
                .builder().text(MarkdownTextObject.builder().text("*ID:* " + workflowId
                    + "\n*Name:* " + workflowName + "\n*Summary:* " + workflowSummary).build())
                .build());
    } else {
      blocks.add(SectionBlock.builder().blockId("workflow_fields")
          .text(MarkdownTextObject.builder().text(
              ":slightly_frowning_face: Unfortunately we are unable to find a Workflow with the specified ID ("
                  + workflowId + "), or you do not have access.")
              .build())
          .build());
    }
    blocks.add(DividerBlock.builder().build());
    List<ContextBlockElement> elementsList = new LinkedList<>();
    elementsList.add(MarkdownTextObject.builder().text(
        MODAL_TEXT_FOOTER)
        .build());
    blocks.add(ContextBlock.builder().elements(elementsList).build());
    return blocks;
  }

  /*
   * Processes the Submitted Run Modal from Slack, executes the Workflow,
   * and responds with a message to the user
   * 
   * <h4>Specifications</h4>
   * <ul>
   * <li><a href="https://api.slack.com/methods/chat.postMessage#formatting">Slack chat.postMessage API</a></li>
   * </ul>
   * 
   * @param jsonPayload     the mapped payload from the interactivity endpoint
   * @return Supplier       To be used by the CompletableFuture
   */
  public Supplier<Boolean> executeRunModal(JsonNode jsonPayload) {
    return () -> {
      // public SlackResponseActionModel executeRunModal(JsonNode jsonPayload) {
      Exception exception = null;
      final String userId = jsonPayload.get("user").get("id").asText();
      final String workflowId = jsonPayload.get("view").get("private_metadata").asText();
      final String teamId = jsonPayload.get("user").get("id").asText();
      final String authToken = getSlackAuthToken(teamId);

      Slack slack = Slack.getInstance();
      try {
        UsersInfoResponse userInfo =
            slack.methods(authToken).usersInfo(UsersInfoRequest.builder().user(userId).build());
        LOGGER.debug("User Info: " + userInfo.toString());
        if (userInfo != null && userInfo.getUser() != null && userInfo.getUser().getProfile() != null) {
          // Trigger workflow Execution and impersonate Slack user
          String userEmail = userInfo.getUser().getProfile().getEmail();
          if (userEmail != null && canExecuteWorkflow(workflowId, userEmail)) {
            FlowActivity flowActivity =
                executionService.executeWorkflow(workflowId, Optional.empty(), Optional.empty(), Optional.empty());
            LOGGER.debug(flowActivity.toString());

            ChatPostMessageResponse messageResponse = slack.methods(authToken)
                .chatPostMessage(req -> req.channel(userId)
                    .blocks(activityBlocks(workflowId, flowActivity.getWorkflowName(),
                        flowActivity.getShortDescription(), flowActivity.getId())));
            LOGGER.debug(messageResponse.toString());
          } else {
            throw new RunWorkflowException(":slightly_frowning_face: Unfortunately we are unable to find a Workflow with the specified ID ("
                + workflowId + "), or you do not have access.");
          }
        } else {
          throw new RunWorkflowException(
              "Unable to retrieve a matching User Profile to use when executing the Workflow.");
        }
      } catch (RunWorkflowException | IOException | SlackApiException | HttpClientErrorException
          | BoomerangException e) {
        LOGGER.error(e);
        exception = e;
      }

      if (exception != null) {
        try {
          String message = exception.getMessage();
          ChatPostMessageResponse messageResponse = slack.methods(authToken)
              .chatPostMessage(req -> req.channel(userId).blocks(activityErrorBlocks(message)));
          LOGGER.debug(messageResponse.toString());
        } catch (IOException | SlackApiException e2) {
          LOGGER.error(e2);
        }
        return false;
      }

      return true;
    };
  }
  
  private Boolean canExecuteWorkflow(String workflowId, String email) {
    if (filterService.getFilteredWorkflowIdsForUserEmail(Optional.empty(), Optional.empty(),
        Optional.empty(), email).contains(workflowId)) {
      LOGGER.debug("Can execute Workflow");
      return true;
    }
    LOGGER.debug("No matching workflow found for User");
    return false;
  }
  
  /*
   * Creates the Blocks that are used with returning an error to the User
   */
  private List<LayoutBlock> activityErrorBlocks(String message) {
    List<LayoutBlock> blocks = new LinkedList<>();
    blocks.add(HeaderBlock.builder()
        .text(PlainTextObject.builder().text("Workflow Activity").build()).build());
    blocks.add(SectionBlock.builder().blockId("workflow_title")
        .text(PlainTextObject.builder()
            .text(":slightly_frowning_face: " + message).build())
        .build());
    blocks.add(DividerBlock.builder().build());
    List<ContextBlockElement> elementsList = new LinkedList<>();
    elementsList.add(MarkdownTextObject.builder().text(
        ":bulb: This integration is in _alpha_ and currently only works with Workflows that do not require parameters to be entered.")
        .build());
    blocks.add(ContextBlock.builder().elements(elementsList).build());
    return blocks;
  }

  /*
   * Creates the Blocks that are part of the Workflow Activity Slack message after a Run Modal is
   * submitted in slack
   */
  private List<LayoutBlock> activityBlocks(String workflowId, String workflowName,
      String workflowSummary, String activityId) {
    List<LayoutBlock> blocks = new LinkedList<>();
    blocks.add(HeaderBlock.builder()
        .text(PlainTextObject.builder().text("Workflow Activity").build()).build());
    blocks.add(SectionBlock.builder().blockId("workflow_title")
        .text(PlainTextObject.builder()
            .text("You requested a workflow to be run with the following details.").build())
        .build());
    blocks
        .add(
            SectionBlock
                .builder().text(MarkdownTextObject.builder().text("*ID:* " + workflowId
                    + "\n*Name:* " + workflowName + "\n*Summary:* " + workflowSummary).build())
                .build());
    List<BlockElement> buttonsList = new LinkedList<>();
    buttonsList.add(ButtonElement.builder()
        .text(PlainTextObject.builder().text(":dart: View Activity").emoji(true).build())
        .url(flowAppsUrl + "/activity/" + workflowId + "/execution/" + activityId).build());
    // TODO: add additional actions such as documentation.
    blocks.add(ActionsBlock.builder().elements(buttonsList).build());
    blocks.add(DividerBlock.builder().build());
    List<ContextBlockElement> elementsList = new LinkedList<>();
    elementsList.add(MarkdownTextObject.builder().text(
        MODAL_TEXT_FOOTER)
        .build());
    blocks.add(ContextBlock.builder().elements(elementsList).build());
    return blocks;
  }
  
  /*
   * Handles the Slack OAuth2 redirect flow. Upon successful code / token exchange will
   * redirect user to the slack app in the workspace.
   */
  public ResponseEntity<?> handleAuth(String code) {
    final String appId =
        flowSettingsService.getConfiguration("extensions", "slack.appId").getValue();
    final String clientId =
        flowSettingsService.getConfiguration("extensions", "slack.clientId").getValue();
    final String clientSecret =
        flowSettingsService.getConfiguration("extensions", "slack.clientSecret").getValue();

    Slack slack = Slack.getInstance();
    try {
      OAuthV2AccessResponse authResponse = slack.methods().oauthV2Access(req -> req.clientId(clientId).clientSecret(clientSecret).code(code));
      LOGGER.debug("Auth Response: " + authResponse.toString());
      if (authResponse.isOk()) {
        saveSlackAuthToken(authResponse);
      }
      //TODO: return different response if not ok and redirect somewhere else.
      return ResponseEntity.status(HttpStatus.FOUND).location(new URI("slack://app?team=" + authResponse.getTeam().getId() + "&id=" + appId)).build();
    } catch (IOException | SlackApiException | URISyntaxException e) {
      LOGGER.error(e.toString());
    }
    
    return ResponseEntity.ok().build();
  }
  
  /*
   * Helper method to save the Slack Auth. It will check if the team already has an auth object.
   * 
   * @param OAuthV2AccessResponse
   */
  private void saveSlackAuthToken(OAuthV2AccessResponse authResponse) {
    ExtensionEntity authExtension = new ExtensionEntity();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> payload =
        mapper.convertValue(authResponse, new TypeReference<Map<String, Object>>() {});
    KeyValuePair teamIdLabel = new KeyValuePair("teamId", authResponse.getTeam().getId());
    
//    Optional<ExtensionEntity> origAuthExtension = extensionsRepository.findByType(EXTENSION_TYPE).stream()
//        .filter(e -> e.getLabels().indexOf(teamIdLabel) != -1).findFirst();
    List<ExtensionEntity> authsList = checkExistingAuthExtensions(authResponse.getTeam().getId());
    if (!authsList.isEmpty()) {
      LOGGER.debug("Overriding existing Slack Team Auth");
      authExtension = authsList.get(0);
      authExtension.setData(payload);
    } else {
      LOGGER.debug("Saving new Slack Team Auth");
      authExtension.setType(EXTENSION_TYPE);
      authExtension.setData(payload);
      List<KeyValuePair> labels = new LinkedList<>();
      labels.add(teamIdLabel);
      authExtension.setLabels(labels);
    }
    extensionsRepository.save(authExtension);
    LOGGER.debug(authExtension.toString());
  }

  /*
   * Helper method to retrieve a saved Auth for a Slack Team (org)
   */
  private List<ExtensionEntity> checkExistingAuthExtensions(String teamId) {
    List<ExtensionEntity> authExtensions = new LinkedList<>();
    extensionsRepository.findByType(EXTENSION_TYPE).forEach(e -> {
      Map<String, String> executionProperties = ParameterMapper.keyValuePairListToMap(e.getLabels());
      if (executionProperties.containsKey("teamId") && teamId.equals(executionProperties.get("teamId"))) {
        authExtensions.add(e);
        LOGGER.debug("Found matching Slack Team Auth");
      }
    });
    return authExtensions;
  }
  
  /*&
   * Helper method to retrieve the stored Slack Token using the extensions generic collection
   * 
   * @param teamId
   */
  private String getSlackAuthToken(String teamId) {
    List<ExtensionEntity> authsList = checkExistingAuthExtensions(teamId);
    if (!authsList.isEmpty()) {
        String teamAuthToken = authsList.get(0).getData().get("accessToken").toString();
        LOGGER.debug("Using existing team Slack auth token: " + teamAuthToken);
        return teamAuthToken;
    }
    String defaultAuthToken = flowSettingsService.getConfiguration("extensions", "slack.token").getValue();
    LOGGER.debug("Using default Slack auth token: " + defaultAuthToken);
    return defaultAuthToken;
  }
  
  /*
   * Scaffold for a future Interactivity Method
   * 
   * <h4>Specifications</h4>
   * <ul>
   * <li><a href=""></a></li>
   * </ul>
   * 
   * @param jsonPayload     the mapped payload from the interactivity endpoint
   * @return Supplier       To be used by the CompletableFuture
   */
//  public Supplier<Boolean> executeRunModal(JsonNode jsonPayload) {
//    return () -> {
      // public SlackResponseActionModel executeRunModal(JsonNode jsonPayload) {

      // 
      // View updatedView = View.builder().type("modal")
      // .title(ViewTitle.builder().type("plain_text").text("Run Workflow").emoji(true).build())
      // .callbackId("workflow-run-modal").privateMetadata(workflowId).blocks(executeBlocks(workflowId,
      // flowActivity.getWorkflowName(), flowActivity.getShortDescription(), flowActivity.getId()))
      // .close(ViewClose.builder().type("plain_text").text("Close").build()).build();
      // try {
      // ViewsUpdateResponse viewResponse = slack.methods(authToken)
      // .viewsUpdate(req -> req.viewId(rootViewId).view(updatedView));
      // LOGGER.info(viewResponse.toString());
      // } catch (IOException | SlackApiException e) {
      // LOGGER.error(e.toString());
      // return false;
      // }
//      return true;
//    };
//  }
}
