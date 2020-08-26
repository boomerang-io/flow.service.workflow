package net.boomerangplatform.service.runner.misc;

import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.TaskResult;
import net.boomerangplatform.model.controller.TaskConfiguration;
import net.boomerangplatform.model.controller.TaskDeletion;
import net.boomerangplatform.model.controller.TaskTemplate;
import net.boomerangplatform.mongo.entity.FlowTaskExecutionEntity;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.service.FlowSettingsService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityTaskService;

@Service
public class CreateTaskLifecycleService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Autowired
  public FlowWorkflowActivityTaskService taskService;

  @Autowired
  private FlowSettingsService flowSettinigs;

  @Value("${controller.createtask.url}")
  public String createTaskURL;

  public TaskResult submitTask(Task task, String activityId, String workflowName) {

    TaskResult taskResult = new TaskResult();
    FlowTaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActiityId(task.getTaskId(), activityId);

    taskResult.setNode(task.getTaskId());

    final TaskTemplate request = new TaskTemplate();
    request.setTaskId(task.getTaskId());
    request.setWorkflowId(task.getWorkflowId());
    request.setWorkflowName(workflowName);
    request.setWorkflowActivityId(activityId);
    request.setTaskName(task.getTaskName());
    request.setTaskActivityId(task.getTaskActivityId());

    final Map<String, String> map = task.getInputs();
    for (final Map.Entry<String, String> pair : map.entrySet()) {
      map.put(pair.getKey(), pair.getValue());
    }

    request.setProperties(map);

    /* Population task configuration details. */
    TaskDeletion taskDeletion = TaskDeletion.Never;

    TaskConfiguration taskConfiguration = new TaskConfiguration();

    String settingsPolicy =
        this.flowSettinigs.getConfiguration("controller", "job.deletion.policy").getValue();
    if (settingsPolicy != null) {
      taskDeletion = TaskDeletion.valueOf(settingsPolicy);
    }
    taskConfiguration.setDeletion(taskDeletion);
    boolean enableDebug = false;

    String enableDebugFlag =
        this.flowSettinigs.getConfiguration("controller", "enable.debug").getValue();

    if (settingsPolicy != null) {
      enableDebug = Boolean.valueOf(enableDebugFlag).booleanValue();
    }
    taskConfiguration.setDebug(Boolean.valueOf(enableDebug));

    request.setConfiguration(taskConfiguration);

    if (task.getRevision() != null) {
      Revision revision = task.getRevision();
      request.setArguments(revision.getArguments());

      if (revision.getImage() != null && !revision.getImage().isBlank()) {
        request.setImage(revision.getImage());
      } else {
        String workerImage =
            this.flowSettinigs.getConfiguration("controller", "worker.image").getValue();
        request.setImage(workerImage);
      }

      if (revision.getCommand() != null && !revision.getCommand().isBlank()) {
        request.setCommand(revision.getCommand());
      }
    } else {
      taskResult.setStatus(FlowTaskStatus.invalid);
      return taskResult;
    }

    final Date startDate = new Date();

    taskExecution.setStartTime(startDate);
    taskExecution.setFlowTaskStatus(FlowTaskStatus.inProgress);
    taskExecution = taskService.save(taskExecution);

    logPayload("Create Task Request", request);

    try {
      TaskResponse response =
          restTemplate.postForObject(createTaskURL, request, TaskResponse.class);

      if (response != null) {
        taskExecution.setOutputs(response.getOutput());

        logPayload("Create Task Response", response);
      }

      final Date finishDate = new Date();
      final long duration = finishDate.getTime() - startDate.getTime();

      taskExecution.setDuration(duration);
      taskExecution.setFlowTaskStatus(FlowTaskStatus.completed);

      if (response != null && !"0".equals(response.getCode())) {
        taskExecution.setFlowTaskStatus(FlowTaskStatus.failure);
        taskResult.setStatus(FlowTaskStatus.failure);
      } else {
        taskResult.setStatus(taskExecution.getFlowTaskStatus());
      }

      LOGGER.info("Task result: {}", taskResult.getStatus());


    } catch (RestClientException ex) {
      taskExecution.setFlowTaskStatus(FlowTaskStatus.failure);
      taskResult.setStatus(FlowTaskStatus.failure);


      LOGGER.error(ExceptionUtils.getStackTrace(ex));
    }

    taskService.save(taskExecution);
    return taskResult;
  }

  private void logPayload(String payloadName, Object request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writeValueAsString(request);
      LOGGER.info("Received Request :{}", payloadName);
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }

}
