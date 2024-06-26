package io.boomerang.service;

import static io.cloudevents.core.CloudEventUtils.mapData;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.client.EngineClient;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.integrations.service.IntegrationService;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.TriggerEnum;
import io.boomerang.model.enums.ref.ParamType;
import io.boomerang.model.enums.ref.RunStatus;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.RunResult;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunEventRequest;
import io.boomerang.model.ref.WorkflowSubmitRequest;
import io.boomerang.util.ParameterUtil;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;

@Service
public class TriggersServiceImpl implements TriggerService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Value("${flow.workflowrun.auto-start-on-submit}")
  private boolean autoStart;

  @Autowired
  private WorkflowServiceImpl workflowService;

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private IntegrationService integrationService;

  @Autowired
  private RelationshipServiceImpl relationshipServiceImpl;

  /*
   * Receives request and checks if its a supported event. Processing done async.
   * 
   * @return accepted or unprocessable.
   */
  @Override
  public WorkflowRun processEvent(CloudEvent event, Optional<String> workflow) {
    String eventType = event.getType();
    LOGGER.debug("Event Type: " + eventType);
    String eventSubject = event.getSubject();
    LOGGER.debug("Event Subject: " + eventSubject);

    WorkflowSubmitRequest request = new WorkflowSubmitRequest();
    String workflowRef = null;
    if (workflow.isPresent()) {
      workflowRef = workflow.get();
    } else {
      // Assume the WorkflowRef is in the subject as the first element
      workflowRef = eventSubject.replace("/", "").split("/")[0];
    }
    request.setTrigger(TriggerEnum.event);
    request.setParams(eventToRunParams(event));

    LOGGER.debug("Webhook Request: " + request.toString());
    
    // Get the Workflows team
    String teamRef = relationshipServiceImpl.getTeamSlugFromChild(RelationshipType.WORKFLOW, workflowRef);
    if (teamRef.isEmpty()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    // Auto start is not needed when using the default handler
    // As the default handler will pick up the queued Workflow and start the Workflow when ready.
    // However if using the non-default Handler then this may be needed to be set to true.
    return workflowService.submit(teamRef, workflowRef, request, autoStart);
  }

  @Override
  public WorkflowRun processWebhook(String workflowRef,
      JsonNode payload) {
    WorkflowSubmitRequest request = new WorkflowSubmitRequest();
    request.setTrigger(TriggerEnum.webhook);
    request.setParams(payloadToRunParams(payload));

    LOGGER.debug("Webhook Request: " + request.toString());
    
    // Get the Workflows team
    String teamRef = relationshipServiceImpl.getTeamSlugFromChild(RelationshipType.WORKFLOW, workflowRef);
    if (teamRef.isEmpty()) {
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    // Auto start is not needed when using the default handler
    // As the default handler will pick up the queued Workflow and start the Workflow when ready.
    // However if using the non-default Handler then this may be needed to be set to true.
    return workflowService.submit(teamRef, workflowRef, request, autoStart);
  }

  @Override
  public ResponseEntity<?> processGitHubWebhook(String trigger, String eventType, JsonNode payload) {
    LOGGER.debug("GitHub Webhook Request[" + eventType + "]: " + payload.toString());
    
    switch (eventType) {
      case "installation" -> {
        if (payload.get("action") != null) {
          if ("created".equals(payload.get("action").asText())) {
            integrationService.create("github_app", payload.get("installation"));
          } else if ("deleted".equals(payload.get("action").asText())) {
            integrationService.delete("github_app", payload.get("installation"));
          }
          return ResponseEntity.ok().build();
        }
      }
      default -> {        
        // Events that come in will have installation.id and if related to a repo, a repository.name
        LOGGER.debug("Installation ID: " + payload.get("installation").get("id"));
        String teamRef =
            integrationService.getTeamByRef(payload.get("installation").get("id").asText());
        if (!Objects.isNull(teamRef) && !teamRef.isBlank()) {
          WorkflowSubmitRequest request = new WorkflowSubmitRequest();
          request.setTrigger(TriggerEnum.github);
          request.setParams(payloadToRunParams(payload));

          // Auto start is not needed when using the default handler
          // As the default handler will pick up the queued Workflow and start the Workflow when
          // ready.
          // However if using the non-default Handler then this may be needed to be set to true.
          workflowService.internalSubmitForTeam(teamRef, request, autoStart);
        }
        return ResponseEntity.ok().build();
      }
    }
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  @Override
  public void processWFE(String workflowRunId,
      String topic, String status, Optional<JsonNode> payload) {
    WorkflowRunEventRequest request = new WorkflowRunEventRequest();
    request.setTopic(topic);
    request.setStatus(RunStatus.getRunStatus(status));
    if (payload.isPresent()) {
      RunResult data = new RunResult("data", (Object) payload);
      request.setResults(List.of(data));
    }
    engineClient.eventWorkflowRun(workflowRunId, request);
  }

  /*
   * Set the webhook payload as a parameter
   */
  private List<RunParam> payloadToRunParams(JsonNode payload) {
    List<RunParam> params = new LinkedList<>();
    params.add(new RunParam("data", (Object) payload, ParamType.object));
    return params;
  }

  /*
   * Convert the CloudEvent and the event's Data and create params
   */
  private List<RunParam> eventToRunParams(CloudEvent event) {
    List<RunParam> params = new LinkedList<>();
    params.add(new RunParam("event", (Object) event, ParamType.object));

    ObjectMapper mapper = new ObjectMapper();
    PojoCloudEventData<Map<String, Object>> data = mapData(event,
        PojoCloudEventDataMapper.from(mapper, new TypeReference<Map<String, Object>>() {}));
    params.add(new RunParam("data", (Object) data, ParamType.object));
    params.addAll(ParameterUtil.mapToRunParamList(data.getValue()));
    return params;
  }
}
