package io.boomerang.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.enums.ref.ParamType;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunSubmitRequest;
import io.cloudevents.CloudEvent;

@Service
public class ListenerServiceImpl implements ListenerService {

  private static final Logger logger = LogManager.getLogger();

  private static final String TYPE_PREFIX = "io.boomerang.event.";
  
  @Value("${flow.workflowrun.auto-start-on-submit}")
  private boolean autoStart;

  @Autowired
  private WorkflowRunService workflowRunService;
  
  /*
   * Receives request and checks if its a supported event. Processing done async.
   * 
   * @return accepted or unprocessable. 
   */
  @Override
  public ResponseEntity<?> processEvent(CloudEvent event) {
    // Check if event that we support and return with accepted or rejected. Processing will be done async.
    if (event.getType().startsWith(TYPE_PREFIX)) {
//      processAsync(event);
      logger.debug("CloudEvent processed.");
    } else {
      logger.debug("CloudEvent could not be processed.");
      return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
    }
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }
  
  @Override
  public ResponseEntity<WorkflowRun> processWebhook(String trigger, String workflowId, JsonNode payload) {
    WorkflowRunSubmitRequest request = new WorkflowRunSubmitRequest();
    request.setWorkflowRef(workflowId);
    request.setTrigger("webhook");
    request.setParams(payloadToRunParams(payload));
    
    logger.debug("Webhook Request: " + request.toString());
    
    //Auto start is not needed when using the default handler
    //As the default handler will pick up the queued Workflow and start the Workflow when ready.
    //However if using the non-default Handler then this may be needed to be set to true.
    return workflowRunService.submit(request, autoStart);
  }
  
  @Override
  public ResponseEntity<WorkflowRun> processWFE(String workflowId, String workflowRunId, String topic, String status, Optional<JsonNode> payload) {
    //TODO figure out how to update the TaskRun for the WaitForEvent
    return null;
  }
//
//private Future<Boolean> processAsync(CloudEvent event) {
//  Supplier<Boolean> supplier = () -> {
//    Boolean isSuccess = Boolean.FALSE;
//      String eventType = event.getType().substring(TYPE_PREFIX.length());
//      logger.info("Event Type: " + eventType);
//      ObjectMapper mapper = new ObjectMapper();    
//      if ("workflowrun"
//          .toLowerCase().equals(eventType.toLowerCase())) {
//        try {
//          PojoCloudEventData<WorkflowRun> data = mapData(
//              event,
//              PojoCloudEventDataMapper.from(mapper,WorkflowRun.class)
//          );
//          WorkflowRun workflowRun = data.getValue();
//          logger.info(workflowRun.toString());
//          WorkflowRequest request = new WorkflowRequest();
//          request.setWorkflowRef(workflowRun.getWorkflowRef());
//          request.setWorkflowRunRef(workflowRun.getId());
//          request.setLabels(workflowRun.getLabels());
//          request.setWorkspaces(workflowRun.getWorkspaces());
//          if (RunPhase.pending.equals(workflowRun.getPhase()) && RunStatus.ready.equals(workflowRun.getStatus())) {
//            logger.info("Executing WorkflowRun...");
//            workflowService.execute(request);
//            engineClient.startWorkflow(workflowRun.getId());
//          } else if (RunPhase.completed.equals(workflowRun.getPhase())) {
//            logger.info("Finalizing WorkflowRun...");
//            workflowService.terminate(request);
//            engineClient.finalizeWorkflow(workflowRun.getId());
//          }
//        } catch (BoomerangException e) {
//          logger.fatal("A fatal error has occurred while processing the message!", e);
//          //TODO catch failure and end workflow with error status
//        } catch (Exception e) {
//          logger.fatal("A fatal error has occurred while processing the message!", e);
//        }
//      } else if ("taskrun".toLowerCase().equals(eventType.toLowerCase())) {
//        try {
//          PojoCloudEventData<TaskRun> data = mapData(
//              event,
//              PojoCloudEventDataMapper.from(mapper,TaskRun.class)
//          );
//          TaskRun taskRun = data.getValue();
//          logger.info(taskRun.toString());
//          try {
//            if ((TaskType.template.equals(taskRun.getType()) || TaskType.custom.equals(taskRun.getType()) || TaskType.script.equals(taskRun.getType())) && RunPhase.pending.equals(taskRun.getPhase()) && RunStatus.ready.equals(taskRun.getStatus())) {
//              logger.info("Executing TaskRun...");
//              TaskResponse response = new TaskResponse();
//              if (TaskType.template.equals(taskRun.getType())) {
//                TaskTemplate request = new TaskTemplate(taskRun);
//                logger.info(request.toString());
//                engineClient.startTask(taskRun.getId());
//                response = taskService.execute(request);
//              } else if (TaskType.custom.equals(taskRun.getType())) {
//                TaskCustom request = new TaskCustom(taskRun);
//                logger.info(request.toString());
//                engineClient.startTask(taskRun.getId());
//                response = taskService.execute(request);
//              }
//              TaskRunEndRequest endRequest = new TaskRunEndRequest();
//              endRequest.setStatus(RunStatus.succeeded);
//              endRequest.setStatusMessage(response.getMessage());
//              endRequest.setResults(response.getResults());
//              engineClient.endTask(taskRun.getId(), endRequest);
//            } else if ((TaskType.template.equals(taskRun.getType()) || TaskType.custom.equals(taskRun.getType()) || TaskType.script.equals(taskRun.getType())) && RunPhase.completed.equals(taskRun.getPhase()) && (RunStatus.cancelled.equals(taskRun.getStatus()) || RunStatus.timedout.equals(taskRun.getStatus()))) {
//              logger.info("Cancelling TaskRun...");
//              TaskTemplate request = new TaskTemplate(taskRun);
//              taskService.terminate(request);
//            } else {
//              logger.info("Skipping TaskRun as criteria not met; (Type: template, custom, or script), (Status: ready), and (Phase: pending).");
//            }
//          } catch (BoomerangException e) {
//            logger.fatal("Failed to execute TaskRun.", e);
//            TaskRunEndRequest endRequest = new TaskRunEndRequest();
//            endRequest.setStatus(RunStatus.failed);
//            RunError error = new RunError(Integer.toString(e.getCode()), e.getMessage());
//            endRequest.setError(error);
//            engineClient.endTask(taskRun.getId(), endRequest);
//          }
//        } catch (Exception e) {
//          logger.fatal("A fatal error has occurred while processing the message!", e);
//        }
//      isSuccess = Boolean.TRUE;
//    } 
//    return isSuccess;
//  };
//
//  return CompletableFuture.supplyAsync(supplier);
//}
//
////
////  @Override
////  public ResponseEntity<CloudEvent<AttributesImpl, JsonNode>> routeCloudEvent(
////      CloudEvent<AttributesImpl, JsonNode> cloudEvent, String token, URI uri) {
////
////    // Validate Token and WorkflowID. Do first.
////    String subject = cloudEvent.getAttributes().getSubject().orElse("");
////
////    if (!subject.startsWith("/") || cloudEvent.getData().isEmpty()) {
////      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
////    }
////
////    HttpStatus accessStatus = checkAccess(getWorkflowIdFromSubject(subject), token);
////    if (accessStatus != HttpStatus.OK) {
////      return ResponseEntity.status(accessStatus).build();
////    }
////
////    logger.debug("routeCloudEvent() - CloudEvent Attributes: " + cloudEvent.getAttributes());
////    logger.debug("routeCloudEvent() - CloudEvent Data: " + cloudEvent.getData().get());
////
////    String eventId = UUID.randomUUID().toString();
////    String eventType = TYPE_PREFIX + "custom";
////
////    String status = "success";
////    if (cloudEvent.getExtensions() != null && cloudEvent.getExtensions().containsKey("status")) {
////      String statusExtension = cloudEvent.getExtensions().get("status").toString();
////      if ("failure".equals(statusExtension)) {
////        status = statusExtension;
////      }
////    }
////    CustomAttributeExtension statusCAE = new CustomAttributeExtension("status", status);
////
////    // @formatter:off
////    final CloudEventImpl<JsonNode> forwardedCloudEvent = CloudEventBuilder.<JsonNode>builder()
////        .withType(eventType)
////        .withExtension(statusCAE)
////        .withId(eventId)
////        .withSource(uri)
////        .withData(cloudEvent.getData().get())
////        .withSubject(subject)
////        .withTime(ZonedDateTime.now())
////        .build();
////    // @formatter:on
////
////    forwardCloudEvent(forwardedCloudEvent);
////
////    return ResponseEntity.ok().body(forwardedCloudEvent);
////  }
////
////  private String getWorkflowIdFromSubject(String subject) {
////    // Reference 0 will be an empty string as it is the left hand side of the split
////    String[] splitArr = subject.split("/");
////    if (splitArr.length >= 2) {
////      return splitArr[1].toString();
////    } else {
////      logger.error("processCloudEvent() - Error: No workflow ID found in event");
////      return "";
////    }
////  }
  
  /*
  * Loop through a Workflow's parameters and if a JsonPath is set read the event payload and
  * attempt to find a payload.
  * 
  * Notes: - We drop exceptions to ensure Workflow continues executing - We return null if path not
  * found using DEFAULT_PATH_LEAF_TO_NULL.
  * 
  * Reference: - https://github.com/json-path/JsonPath#tweaking-configuration
  */
  private List<RunParam> payloadToRunParams(JsonNode payload) {
//   Configuration jsonConfig = Configuration.builder().mappingProvider(new JacksonMappingProvider())
//       .jsonProvider(new JacksonJsonNodeJsonProvider()).options(Option.DEFAULT_PATH_LEAF_TO_NULL)
//       .build();

   List<RunParam> params = new LinkedList<>();
   params.add(new RunParam("payload", (Object) payload, ParamType.object));
//   DocumentContext jsonContext = JsonPath.using(jsonConfig).parse(payload);
//   if (inputProperties != null) {
//     try {
//       inputProperties.forEach(inputProperty -> {
//         if (inputProperty.getJsonPath() != null && !inputProperty.getJsonPath().isBlank()) {
//  
//           JsonNode propertyValue = jsonContext.read(inputProperty.getJsonPath());
//  
//           if (!propertyValue.isNull()) {
//             String value = propertyValue.toString();
//             value = value.replaceAll("^\"+|\"+$", "");
//             logger.info("processProperties() - Property: " + inputProperty.getKey()
//                 + ", Json Path: " + inputProperty.getJsonPath() + ", Value: " + value);
//             properties.put(inputProperty.getKey(), value);
//           } else {
//             logger.info("processProperties() - Skipping property: " + inputProperty.getKey());
//           }
//         }
//       });
//     } catch (Exception e) {
//       // Log and drop exception. We want the workflow to continue execution.
//       logger.error(e.toString());
//     }
//   }
//   ObjectMapper mapper = new ObjectMapper();
//   Map<String, String> payloadProperties = mapper.convertValue(payload.get("properties"),
//       new TypeReference<Map<String, String>>() {});
//   if (payloadProperties != null) {
//     properties.putAll(payloadProperties);
//   }
  
   // properties.put("eventPayload", eventData.toString());
  
//   WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
//   Map<String, String> finalProperties = new HashMap<>();
//  
//   if (!properties.isEmpty()) {
//     List<KeyValuePair> propertyList = ParameterMapper.mapToKeyValuePairList(properties);
//     Map<String, WorkflowProperty> workflowPropMap = workflow.getProperties().stream().collect(
//         Collectors.toMap(WorkflowProperty::getKey, WorkflowProperty -> WorkflowProperty));
//     // Use default value for password-type parameter when user input value is null when executing
//     // workflow.
//     propertyList.stream().forEach(p -> {
//       if (workflowPropMap.get(p.getKey()) != null
//           && FieldType.PASSWORD.value().equals(workflowPropMap.get(p.getKey()).getType())
//           && p.getValue() == null) {
//         p.setValue(workflowPropMap.get(p.getKey()).getDefaultValue());
//       }
//     });
//  
//     for (KeyValuePair prop : propertyList) {
//       logger.info("processProperties() - " + prop.getKey() + "=" + prop.getValue());
//       finalProperties.put(prop.getKey(), prop.getValue());
//     }
//   } else {
//  
//     for (WorkflowProperty property : workflow.getProperties()) {
//       finalProperties.put(property.getKey(), property.getDefaultValue());
//     }
//   }
   return params;
  }
}
