package io.boomerang.v4.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import io.boomerang.error.BoomerangException;

@Service
@Primary
public class HandlerClientImpl implements HandlerClient {

  private static final Logger LOGGER = LogManager.getLogger();

  @Value("${flow.handler.deleteworkspace.url}")
  public String deleteWorkspaceURL;

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  //TODO move to the handler
//  private void prepareTemplateImageRequest(Task task, TaskResult taskResult,
//      final TaskTemplate request, String activityId,
//      ControllerRequestProperties applicationProperties, Map<String, String> map) {
//    if (task.getRevision() != null) {
//      Revision revision = task.getRevision();
//      request.setArguments(revision.getArguments());
//      List<String> arguments = revision.getArguments();
//
//      arguments = prepareTemplateTaskArguments(arguments, activityId, applicationProperties, map);
//
//      request.setArguments(arguments);
//      if (revision.getImage() != null && !revision.getImage().isBlank()) {
//        request.setImage(revision.getImage());
//      } else {
//        String workerImage =
//            this.flowSettings.getConfiguration("controller", "worker.image").getValue();
//        request.setImage(workerImage);
//      }
//
//      List<String> command = revision.getCommand();
//
//
//      if (command != null && !command.isEmpty() && !checkForBlankValues(command)) {
//        request.setCommand(revision.getCommand());
//        List<String> cmdArgs = new LinkedList<>();
//        for (String line : revision.getCommand()) {
//          String newValue =
//              propertyManager.replaceValueWithProperty(line, activityId, applicationProperties);
//          cmdArgs.add(newValue);
//        }
//
//        request.setCommand(cmdArgs);
//      }
//      if (revision.getScript() != null && !revision.getScript().isBlank()) {
//        request.setScript(revision.getScript());
//      }
//      if (revision.getEnvs() != null) {
//        request.setEnvs(revision.getEnvs());
//      } else {
//        request.setEnvs(new LinkedList<>());
//      }
//      request.setResults(new LinkedList<>());
//
//      if (task.getResults() != null) {
//        request.setResults(task.getResults());
//      } else {
//        if (revision.getResults() != null) {
//          request.setResults(revision.getResults());
//        }
//      }
//
//    } else {
//      taskResult.setStatus(TaskStatus.invalid);
//    }
//  }

  //TODO
  @Override
  public void deleteWorkspace(WorkspaceRequest request) {
    try {
      String url = deleteWorkspaceURL;
      
      LOGGER.info("URL: " + deleteWorkspaceURL);

      ResponseEntity<WorkspaceRequest> response = restTemplate.postForEntity(url, request, WorkspaceRequest.class);

      LOGGER.info("Status Response: " + response.getStatusCode());
      LOGGER.info("Content Response: " + response.getBody().toString());
    } catch (RestClientException ex) {
      LOGGER.error(ex.toString());
      throw new BoomerangException(ex, HttpStatus.INTERNAL_SERVER_ERROR.value(),
          ex.getClass().getSimpleName(), "Exception in communicating with internal services.",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
