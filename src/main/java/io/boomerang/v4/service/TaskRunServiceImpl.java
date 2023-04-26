package io.boomerang.v4.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.Comparator;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.service.DAGTask;
import io.boomerang.service.Dag;
import io.boomerang.service.FlowTaskTemplateEntity;
import io.boomerang.service.ParameterLayers;
import io.boomerang.service.Revision;
import io.boomerang.service.RevisionEntity;
import io.boomerang.service.Task;
import io.boomerang.service.TaskExecutionEntity;
import io.boomerang.service.TaskTemplateConfig;

public class TaskRunServiceImpl implements TaskRunService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Value("${flow.hanlder.streamlogs.url}")
  private String getStreamDownloadPath;

  @Override
  public StreamingResponseBody getTaskRunLog(String workflowRunId, String taskRunId) {

    LOGGER.info("Getting TaskRun log for activity: {} task id: {}", activityId, taskId);

    TaskExecutionEntity taskExecution = taskService.findByTaskIdAndActivityId(taskId, activityId);

    ActivityEntity activity =
        workflowActivityService.findWorkflowActivtyById(taskExecution.getActivityId());

    List<String> removeList = buildRemovalList(taskId, taskExecution, activity);
    LOGGER.debug("Removal List Count: {} ", removeList.size());

    return outputStream -> {
      Map<String, String> requestParams = new HashMap<>();
      requestParams.put("workflowId", activity.getWorkflowId());
      requestParams.put("workflowActivityId", activityId);
      requestParams.put("taskActivityId", taskExecution.getId());
      requestParams.put("labels", taskId);

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
        LOGGER.error("Error downloading logs: {} task id: {}", activityId, taskId);
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

            printWriter.println(satanzieInput(input, maskWordList));
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

  private List<String> buildRemovalList(String taskId, TaskExecutionEntity taskExecution,
      ActivityEntity activity) {

    String activityId = activity.getId();
    List<String> removalList = new LinkedList<>();
    Task task = new Task();
    task.setTaskId(taskId);
    task.setTaskType(taskExecution.getTaskType());

    ParameterLayers applicationProperties =
        propertyManager.buildParameterLayers(task, activityId, activity.getWorkflowId());
    Map<String, String> map = applicationProperties.getMap(false);

    String workflowRevisionId = activity.getWorkflowRevisionid();

    Optional<RevisionEntity> revisionOptional = this.versionService.getRevision(workflowRevisionId);
    if (revisionOptional.isEmpty()) {
      return new LinkedList<>();
    }

    RevisionEntity revision = revisionOptional.get();
    Dag dag = revision.getDag();
    List<DAGTask> dagTasks = dag.getTasks();
    DAGTask dagTask =
        dagTasks.stream().filter((t) -> taskId.equals(t.getTaskId())).findFirst().orElse(null);
    if (dagTask != null) {
      if (dagTask.getTemplateId() != null) {
        FlowTaskTemplateEntity flowTaskTemplateEntity =
            templateService.getTaskTemplateWithId(dagTask.getTemplateId());
        if (flowTaskTemplateEntity != null && flowTaskTemplateEntity.getRevisions() != null) {
          Optional<Revision> latestRevision = flowTaskTemplateEntity.getRevisions().stream()
              .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
          if (latestRevision.isPresent()) {
            Revision rev = latestRevision.get();
            for (TaskTemplateConfig taskConfig : rev.getConfig()) {
              if ("password".equals(taskConfig.getType())) {
                LOGGER.debug("Found a secured property being used: {}", taskConfig.getKey());
                String key = taskConfig.getKey();
                String inputValue = map.get(key);
                if (inputValue == null || inputValue.isBlank()) {
                  inputValue = taskConfig.getDefaultValue();
                }
                String value = propertyManager.replaceValueWithProperty(inputValue, activityId,
                    applicationProperties);
                value = propertyManager.replaceValueWithProperty(value, activityId,
                    applicationProperties);
                LOGGER.debug("New Value: {}", value);
                if (!value.isBlank()) {
                  removalList.add(value);
                }
              }
            }
          }
        }
      }
    }

    LOGGER.debug("Displaying removal list");
    for (String item : removalList) {
      LOGGER.debug("Item: {}", item);
    }
    return removalList;
  }

  private String satanzieInput(String input, List<String> removeList) {
    for (String value : removeList) {
      input = input.replaceAll(Pattern.quote(value), "******");
    }
    return input;
  }

}
