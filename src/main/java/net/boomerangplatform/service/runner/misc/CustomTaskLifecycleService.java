package net.boomerangplatform.service.runner.misc;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.TaskResponse;
import net.boomerangplatform.model.TaskResult;
import net.boomerangplatform.model.controller.TaskConfiguration;
import net.boomerangplatform.model.controller.TaskCustom;
import net.boomerangplatform.model.controller.TaskDeletion;
import net.boomerangplatform.mongo.entity.FlowTaskExecutionEntity;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.mongo.service.FlowSettingsService;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityTaskService;

@Service
public class CustomTaskLifecycleService {

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Autowired
  public FlowWorkflowActivityTaskService taskService;

  @Value("${controller.createtask.url}")
  public String createURL;

  @Autowired
  private FlowSettingsService flowSettinigs;

  public TaskResult submitCustomTask(Task task, String activityId, String workflowName) {

    TaskResult taskResult = new TaskResult();
    FlowTaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActiityId(task.getTaskId(), activityId);

    taskResult.setNode(task.getTaskId());

    final TaskCustom request = new TaskCustom();
    request.setTaskId(task.getTaskId());
    request.setWorkflowId(task.getWorkflowId());
    request.setWorkflowName(workflowName);
    request.setWorkflowActivityId(activityId);
    request.setTaskName(task.getTaskName());
    request.setTaskActivityId(task.getTaskActivityId());

    final Map<String, String> map = task.getInputs();


    request.setImage(map.get("image"));
    request.setCommand(map.get("command"));

    List<String> args = new LinkedList<>();

    if (map.get("arguments") != null) {
      String arguments = map.get("arguments");
      if (!arguments.isBlank()) {
        String[] lines = arguments.split("\\r?\\n");
        args.addAll(Arrays.asList(lines));
      }
    }
    request.setArguments(args);

    final Date startDate = new Date();

    taskExecution.setStartTime(startDate);
    taskExecution.setFlowTaskStatus(FlowTaskStatus.inProgress);
    taskExecution = taskService.save(taskExecution);

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

    try {

      TaskResponse response = restTemplate.postForObject(createURL, request, TaskResponse.class);

      if (response != null) {
        taskExecution.setOutputs(response.getOutput());
      }

      final Date finishDate = new Date();
      final long duration = finishDate.getTime() - startDate.getTime();

      taskExecution.setDuration(duration);
      taskExecution.setFlowTaskStatus(FlowTaskStatus.completed);

      if (response != null && !"0".equals(response.getCode())) {
        taskExecution.setFlowTaskStatus(FlowTaskStatus.failure);
      } else {
        taskResult.setStatus(taskExecution.getFlowTaskStatus());
      }
    } catch (RestClientException ex) {
      taskExecution.setFlowTaskStatus(FlowTaskStatus.failure);
      taskResult.setStatus(FlowTaskStatus.failure);

    }

    taskService.save(taskExecution);
    return taskResult;
  }

}
