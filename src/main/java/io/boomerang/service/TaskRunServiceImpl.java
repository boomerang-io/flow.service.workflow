package io.boomerang.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.client.EngineClient;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.ref.ParamLayers;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.TaskRun;

@Service
public class TaskRunServiceImpl implements TaskRunService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Value("${flow.hanlder.streamlogs.url}")
  private String getStreamDownloadPath;

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Autowired
  private EngineClient engineClient;

  @Autowired
  private ParameterManager parameterManager;

  @Autowired
  private RelationshipService relationshipService;

  @Override
  public StreamingResponseBody getTaskRunLog(String workflowRunId, String taskRunId) {
    if (workflowRunId == null || workflowRunId.isBlank() || taskRunId == null || taskRunId.isBlank()) {
      //TODO better error message
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }
    
    Optional<RelationshipEntity> rel = relationshipService.getRelationship(RelationshipRef.WORKFLOWRUN, workflowRunId, RelationshipType.BELONGSTO);
    if (rel.isEmpty()) {
      //TODO better error message
      throw new BoomerangException(BoomerangError.WORKFLOW_RUN_INVALID_REF);
    }

    LOGGER.info("Getting TaskRun log for activity: {} task id: {}", workflowRunId, taskRunId);
    
    TaskRun taskRun = engineClient.getTaskRun(taskRunId);

    List<String> removeList = buildRemovalList(rel.get().getToRef(), workflowRunId, taskRunId, taskRun);
    LOGGER.debug("Removal List Count: {} ", removeList.size());

    //TODO come back and making this work with the new Handler request
    return outputStream -> {
      Map<String, String> requestParams = new HashMap<>();
//      requestParams.put("workflowId", activity.getWorkflowId());
//      requestParams.put("workflowActivityId", activityId);
//      requestParams.put("taskActivityId", taskExecution.getId());
//      requestParams.put("labels", taskId);

      String encodedURL =
          requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(
              Collectors.joining("&", getStreamDownloadPath + "?", ""));

      RequestCallback requestCallback = request -> request.getHeaders()
          .setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

      PrintWriter printWriter = new PrintWriter(outputStream);

      ResponseExtractor<Void> responseExtractor =
          getResponseExtractorForRemovalList(removeList, outputStream, printWriter);
      LOGGER.info("Startingg log download: {}", encodedURL);
      try {
        restTemplate.execute(encodedURL, HttpMethod.GET, requestCallback, responseExtractor);
      } catch (Exception ex) {
        LOGGER.error("Error downloading logs: {} task id: {}", workflowRunId, taskRunId);
        LOGGER.error(ExceptionUtils.getStackTrace(ex));
      }

      LOGGER.info("Completed log download: {}", encodedURL);
    };
  }
  


  private ResponseExtractor<Void> getResponseExtractorForRemovalList(List<String> maskWordList,
      OutputStream outputStream, PrintWriter printWriter) {
    if (maskWordList.isEmpty()) {
      LOGGER.info("Remove word list empty, moving on.");
      return restTemplateResponse -> {
        InputStream is = restTemplateResponse.getBody();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
          outputStream.write(data, 0, nRead);
        }
        return null;
      };
    } else {
      LOGGER.info("Streaming response from controller and processing");
      return restTemplateResponse -> {
        try {
          InputStream is = restTemplateResponse.getBody();
          Reader reader = new InputStreamReader(is);
          BufferedReader bufferedReader = new BufferedReader(reader);
          String input = null;
          while ((input = bufferedReader.readLine()) != null) {

            printWriter.println(sanitizeInput(input, maskWordList));
            if (!input.isBlank()) {
              printWriter.flush();
            }
          }
        } catch (Exception e) {
          LOGGER.error("Error streaming logs, displaying exception and moving on.");
          LOGGER.error(ExceptionUtils.getStackTrace(e));
        } finally {
          printWriter.close();
        }
        return null;
      };
    }
  }

  //TODO: verify this - i doubt we need to do it this way anymore. The TaskRun has the resolved parameters. So we can just check type and value and replace.
  private List<String> buildRemovalList(String teamId, String workflowRunId, String taskRunId, TaskRun taskRun) {
    List<String> removalList = new LinkedList<>();
    //TODO: it has to be workflowId not workflowRunId
    ParamLayers paramLayers =
        parameterManager.buildParamLayers(teamId, workflowRunId);
    Map<String, Object> map = paramLayers.getFlatMap();

    if (taskRun != null && taskRun.getParams() != null) {
      for (RunParam param : taskRun.getParams()) {
        if ("password".equals(param.getType().toString())) {
          LOGGER.debug("Found a secured property being used - Name: {}", param.getName());
          // Assume that Password types are of String Param type
          String value = (String) map.get(param.getName());
          if (!value.isBlank()) {
            removalList.add(value);
          }
        }
      }
    }

    LOGGER.debug("Param removal list");
    for (String item : removalList) {
      LOGGER.debug("Item: {}", item);
    }
    return removalList;
  }

  private String sanitizeInput(String input, List<String> removeList) {
    for (String value : removeList) {
      input = input.replaceAll(Pattern.quote(value), "******");
    }
    return input;
  }

}
