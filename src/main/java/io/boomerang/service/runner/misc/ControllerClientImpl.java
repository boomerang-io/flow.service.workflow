package io.boomerang.service.runner.misc;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.errors.model.BoomerangError;
import io.boomerang.errors.model.ErrorDetail;
import io.boomerang.model.Task;
import io.boomerang.model.TaskResult;
import io.boomerang.model.controller.Response;
import io.boomerang.model.controller.Storage;
import io.boomerang.model.controller.TaskConfiguration;
import io.boomerang.model.controller.TaskDeletion;
import io.boomerang.model.controller.TaskResponse;
import io.boomerang.model.controller.TaskResponseResult;
import io.boomerang.model.controller.TaskTemplate;
import io.boomerang.model.controller.Workflow;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.internal.InternalTaskResponse;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.FlowSettingsService;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.service.PropertyManager;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.refactor.ControllerRequestProperties;
import io.boomerang.service.refactor.TaskClient;
import io.boomerang.service.refactor.TaskService;

@Service
@Primary
public class ControllerClientImpl implements ControllerClient {

  private static final Logger LOGGER = LogManager.getLogger();

  @Value("${controller.createtask.url}")
  public String createTaskURL;

  @Value("${controller.createworkflow.url}")
  private String createWorkflowURL;

  @Autowired
  private FlowSettingsService flowSettinigs;

  @Autowired
  private PropertyManager propertyManager;

  @Autowired
  @Qualifier("internalRestTemplate")
  public RestTemplate restTemplate;

  @Autowired
  public ActivityTaskService taskService;

  @Autowired
  public FlowActivityService activityService;

  @Autowired
  private FlowWorkflowActivityService workflowActivityService;

  @Value("${controller.terminateworkflow.url}")
  private String terminateWorkflowURL;

  @Value("${controller.terminatetask.url}")
  private String terminateTaskURL;

  private static final String CREATEWORKFLOWREQUEST = "Create Workflow Request";
  private static final String TERMINATEWORKFLOWREQUEST = "Terminate Workflow Request";
  private static final String CREATETEMPLATETASKREQUEST = "Create Template Task Request";
  private static final String CREATECUSTOMTASKREQUEST = "Create Custom Task Request";
  private static final String TERMINATETASKREQUEST = "Terminate Task Request";
  private static final String ERRORLOGPRFIX = "Error for: {}";

  @Override
  public boolean createFlow(String workflowId, String workflowName, String activityId,
      boolean enableStorage, List<KeyValuePair> labels, Map<String, String> properties) {


    final Workflow request = new Workflow();
    request.setWorkflowActivityId(activityId);
    request.setWorkflowName(workflowName);
    request.setWorkflowId(workflowId);
    request.setParameters(properties);


    final Storage storage = new Storage();
    storage.setEnable(enableStorage);

    String storageClassName =
        this.flowSettinigs.getConfiguration("workflow", "storage.class").getValue();
    if (storageClassName != null && !storageClassName.isBlank()) {
      storage.setClassName(storageClassName);
    }

    String storageAccessMode =
        this.flowSettinigs.getConfiguration("workflow", "storage.accessMode").getValue();
    if (storageAccessMode != null && !storageAccessMode.isBlank()) {
      storage.setAccessMode(storageAccessMode);
    }

    String storageDefaultSize =
        this.flowSettinigs.getConfiguration("workflow", "storage.size").getValue();
    if (storageDefaultSize != null && !storageDefaultSize.isBlank()) {
      storage.setSize(storageDefaultSize);
    }


    request.setWorkflowStorage(storage);
    request.setLabels(this.convertToMap(labels));

    logPayload(CREATEWORKFLOWREQUEST, request);
    Date startTime = new Date();
    ActivityEntity activity = this.activityService.findWorkflowActivity(activityId);
    try {
      Response response = restTemplate.postForObject(createWorkflowURL, request, Response.class);

      if (response != null && !"0".equals(response.getCode())) {

        ErrorResponse error = new ErrorResponse();
        error.setCode(response.getCode());
        error.setMessage(response.getMessage());
        activity.setError(error);

      }

    } catch (HttpStatusCodeException statusCodeException) {
      LOGGER.error(ExceptionUtils.getStackTrace(statusCodeException));

      String body = statusCodeException.getResponseBodyAsString();
      LOGGER.error("Error Creating Workflow Response Body: {}", body);

      ObjectMapper mapper = new ObjectMapper();
      try {
        BoomerangError controllerError = mapper.readValue(body, BoomerangError.class);
        if (controllerError != null && controllerError.getError() != null) {
          ErrorDetail detail = controllerError.getError();
          ErrorResponse error = new ErrorResponse();
          error.setCode(String.valueOf(detail.getCode()));
          error.setMessage(detail.getDescription());
          activity.setError(error);
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    } catch (RestClientException ex) {
      LOGGER.error(ERRORLOGPRFIX, CREATEWORKFLOWREQUEST);
      LOGGER.error(ExceptionUtils.getStackTrace(ex));
      return false;
    }

    this.workflowActivityService.saveWorkflowActivity(activity);

    Date endTime = new Date();
    logRequestTime(CREATEWORKFLOWREQUEST, startTime, endTime);
    return true;
  }

  @Override
  public boolean terminateFlow(String workflowId, String workflowName, String activityId) {
    final Workflow request = new Workflow();
    request.setWorkflowActivityId(activityId);
    request.setWorkflowName(workflowName);
    request.setWorkflowId(workflowId);
    final Storage storage = new Storage();
    storage.setEnable(true);
    request.setWorkflowStorage(storage);
    logPayload(TERMINATEWORKFLOWREQUEST, request);

    Date startTime = new Date();
    try {
      restTemplate.postForObject(terminateWorkflowURL, request, String.class);
    } catch (RestClientException ex) {
      LOGGER.error(ERRORLOGPRFIX, TERMINATEWORKFLOWREQUEST);
      LOGGER.error(ExceptionUtils.getStackTrace(ex));
      return false;
    }
    Date endTime = new Date();
    logRequestTime(TERMINATEWORKFLOWREQUEST, startTime, endTime);
    return true;
  }

  

  
  @Override
  @Async("flowAsyncExecutor")
  public void submitCustomTask(TaskService t, TaskClient flowTaskClient, Task task, String activityId, String workflowName,
      List<KeyValuePair> labels) {

    

    TaskResult taskResult = new TaskResult();
    TaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActivityId(task.getTaskId(), activityId);

    ActivityEntity activity = this.activityService.findWorkflowActivity(activityId);

    if (activity.getLabels() != null) {
      labels.addAll(activity.getLabels());
    }

    taskResult.setNode(task.getTaskId());
    final TaskTemplate request = new TaskTemplate();
    request.setTaskId(task.getTaskId());
    request.setWorkflowId(task.getWorkflowId());
    request.setWorkflowName(workflowName);
    request.setWorkflowActivityId(activityId);
    request.setTaskName(task.getTaskName());
    request.setTaskActivityId(task.getTaskActivityId());
    request.setLabels(this.convertToMap(labels));

    ControllerRequestProperties applicationProperties =
        propertyManager.buildRequestPropertyLayering(task, activityId, task.getWorkflowId());

    Map<String, String> map = applicationProperties.getMap(true);
    String image = applicationProperties.getLayeredProperty("image");
    image = propertyManager.replaceValueWithProperty(image, activityId, applicationProperties);
    request.setImage(image);

    String command = applicationProperties.getLayeredProperty("command");
    command = propertyManager.replaceValueWithProperty(command, activityId, applicationProperties);

    if (command != null && !command.isBlank()) {
      String[] lines = command.split("\\r?\\n");
      List<String> cmdArgs = new LinkedList<>();
      for (String line : lines) {
        String newValue =
            propertyManager.replaceValueWithProperty(line, activityId, applicationProperties);
        cmdArgs.add(newValue);
      }
      request.setCommand(cmdArgs);
    }

    String script = applicationProperties.getLayeredProperty("shellScript");
    if (script != null) {
      script = propertyManager.replaceValueWithProperty(script, activityId, applicationProperties);
      request.setScript(script);
    }

    List<String> args = prepareCustomTaskArguments(activityId, applicationProperties, map);
    request.setArguments(args);
    final Date startDate = new Date();
    taskExecution.setStartTime(startDate);
    taskExecution.setFlowTaskStatus(TaskStatus.inProgress);
    taskExecution = taskService.save(taskExecution);


    TaskConfiguration taskConfiguration = buildTaskConfiguration();
    request.setConfiguration(taskConfiguration);
    request.setWorkspaces(activity.getTaskWorkspaces());


    logPayload("Create Task Request", request);

    Map<String, String> outputProperties = new HashMap<>();

    try {

      Date startTime = new Date();
      TaskResponse response =
          restTemplate.postForObject(createTaskURL, request, TaskResponse.class);

      Date endTime = new Date();

      logRequestTime(CREATECUSTOMTASKREQUEST, startTime, endTime);
      if (response != null) {
        this.logPayload(CREATECUSTOMTASKREQUEST, response);
        if (response.getResults() != null && !response.getResults().isEmpty()) {
          for (TaskResponseResult result : response.getResults()) {
            String key = result.getName();
            String value = result.getValue();
            outputProperties.put(key, value);
          }
        }
      }
      final Date finishDate = new Date();
      final long duration = finishDate.getTime() - startDate.getTime();
      taskExecution.setDuration(duration);
      taskExecution.setFlowTaskStatus(TaskStatus.completed);

      if (response != null && !"0".equals(response.getCode())) {
        taskExecution.setFlowTaskStatus(TaskStatus.failure);
        ErrorResponse error = new ErrorResponse();
        error.setCode(response.getCode());
        error.setMessage(response.getMessage());
        taskExecution.setError(error);

      } else {
        taskResult.setStatus(taskExecution.getFlowTaskStatus());
      }
    } catch (HttpStatusCodeException statusCodeException) {
      LOGGER.error(ExceptionUtils.getStackTrace(statusCodeException));
      taskExecution.setFlowTaskStatus(TaskStatus.failure);
      taskResult.setStatus(TaskStatus.failure);
      String body = statusCodeException.getResponseBodyAsString();
      LOGGER.error("Error Response Body: {}", body);

      ObjectMapper mapper = new ObjectMapper();
      try {
        BoomerangError controllerError = mapper.readValue(body, BoomerangError.class);
        if (controllerError != null && controllerError.getError() != null) {
          ErrorDetail detail = controllerError.getError();
          ErrorResponse error = new ErrorResponse();
          error.setCode(String.valueOf(detail.getCode()));
          error.setMessage(detail.getDescription());
          taskExecution.setError(error);
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    } catch (RestClientException ex) {
      taskExecution.setFlowTaskStatus(TaskStatus.failure);
      taskResult.setStatus(TaskStatus.failure);
      LOGGER.error(ERRORLOGPRFIX, CREATECUSTOMTASKREQUEST);
      LOGGER.error(ExceptionUtils.getStackTrace(ex));
    }

    taskService.save(taskExecution);
    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(taskExecution.getFlowTaskStatus());
    response.setOutputProperties(outputProperties);

    flowTaskClient.endTask(t, response);
  }


  private List<String> prepareTemplateTaskArguments(List<String> lines, String activityId,
      ControllerRequestProperties applicationProperties, Map<String, String> map) {
    List<String> args = new LinkedList<>();

    for (String line : lines) {
      String newValue =
          propertyManager.replaceValueWithProperty(line, activityId, applicationProperties);
      args.add(newValue);
    }

    return args;
  }


  private List<String> prepareCustomTaskArguments(String activityId,
      ControllerRequestProperties applicationProperties, Map<String, String> map) {
    List<String> args = new LinkedList<>();
    if (map.get("arguments") != null) {
      String arguments = applicationProperties.getLayeredProperty("arguments");
      if (!arguments.isBlank()) {
        String[] lines = arguments.split("\\r?\\n");
        args = new LinkedList<>();
        for (String line : lines) {
          String newValue =
              propertyManager.replaceValueWithProperty(line, activityId, applicationProperties);
          args.add(newValue);
        }
      }
    }
    return args;
  }



  @Override
  @Async("flowAsyncExecutor")
  public void submitTemplateTask(TaskService t, TaskClient flowTaskClient,Task task, String activityId, String workflowName,
      List<KeyValuePair> labels) {

    ActivityEntity activity = this.activityService.findWorkflowActivity(activityId);

    if (activity.getLabels() != null) {
      labels.addAll(activity.getLabels());
    }
    
    TaskResult taskResult = new TaskResult();
    TaskExecutionEntity taskExecution =
        taskService.findByTaskIdAndActivityId(task.getTaskId(), activityId);

    taskResult.setNode(task.getTaskId());

    final TaskTemplate request = new TaskTemplate();
    request.setTaskId(task.getTaskId());
    request.setWorkflowId(task.getWorkflowId());
    request.setWorkflowName(workflowName);
    request.setWorkflowActivityId(activityId);
    request.setTaskName(task.getTaskName());
    request.setTaskActivityId(task.getTaskActivityId());
    request.setLabels(this.convertToMap(labels));


    ControllerRequestProperties applicationProperties =
        propertyManager.buildRequestPropertyLayering(task, activityId, task.getWorkflowId());

    Map<String, String> map = applicationProperties.getTaskInputProperties();

    request.setParameters(map);


    TaskConfiguration taskConfiguration = buildTaskConfiguration();
    request.setConfiguration(taskConfiguration);

    prepareTemplateImageRequest(task, taskResult, request, activityId, applicationProperties, map);

    final Date startDate = new Date();

    taskExecution.setStartTime(startDate);
    taskExecution.setFlowTaskStatus(TaskStatus.inProgress);
    taskExecution = taskService.save(taskExecution);

    request.setWorkspaces(activity.getTaskWorkspaces());
    Map<String, String> outputProperties = new HashMap<>();

    logPayload(CREATETEMPLATETASKREQUEST, request);
    try {

      Date startTime = new Date();

      TaskResponse response =
          restTemplate.postForObject(createTaskURL, request, TaskResponse.class);


      Date endTime = new Date();

      logRequestTime(CREATETEMPLATETASKREQUEST, startTime, endTime);

      if (response != null) {
        this.logPayload(CREATETEMPLATETASKREQUEST, response);
        if (response.getResults() != null && !response.getResults().isEmpty()) {
          response.getResults().get(0);
          if (response.getResults() != null) {
            for (TaskResponseResult result : response.getResults()) {
              String key = result.getName();
              String value = result.getValue();
              outputProperties.put(key, value);
            }
          }
        }
      }

      final Date finishDate = new Date();
      final long duration = finishDate.getTime() - startDate.getTime();

      taskExecution.setDuration(duration);
      taskExecution.setFlowTaskStatus(TaskStatus.completed);
      if (response != null && !"0".equals(response.getCode())) {
        taskExecution.setFlowTaskStatus(TaskStatus.failure);
        taskResult.setStatus(TaskStatus.failure);

        ErrorResponse error = new ErrorResponse();
        error.setCode(response.getCode());
        error.setMessage(response.getMessage());
        taskExecution.setError(error);
      } else {
        taskResult.setStatus(taskExecution.getFlowTaskStatus());
      }
      LOGGER.info("Task result: {}", taskResult.getStatus());
    } catch (HttpStatusCodeException statusCodeException) {
      LOGGER.error(ExceptionUtils.getStackTrace(statusCodeException));
      taskExecution.setFlowTaskStatus(TaskStatus.failure);
      taskResult.setStatus(TaskStatus.failure);
      String body = statusCodeException.getResponseBodyAsString();
      LOGGER.error("Error Response Body: {}", body);

      ObjectMapper mapper = new ObjectMapper();
      try {
        BoomerangError controllerError = mapper.readValue(body, BoomerangError.class);
        if (controllerError != null && controllerError.getError() != null) {
          ErrorDetail detail = controllerError.getError();
          ErrorResponse error = new ErrorResponse();
          error.setCode(String.valueOf(detail.getCode()));
          error.setMessage(detail.getDescription());
          taskExecution.setError(error);
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    } catch (RestClientException ex) {
      taskExecution.setFlowTaskStatus(TaskStatus.failure);
      taskResult.setStatus(TaskStatus.failure);

      LOGGER.error(ERRORLOGPRFIX, CREATETEMPLATETASKREQUEST);
      LOGGER.error(ExceptionUtils.getStackTrace(ex));

    }


    taskService.save(taskExecution);

    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(taskExecution.getFlowTaskStatus());
    response.setOutputProperties(outputProperties);

    flowTaskClient.endTask(t, response);
  }

  private void prepareTemplateImageRequest(Task task, TaskResult taskResult,
      final TaskTemplate request, String activityId,
      ControllerRequestProperties applicationProperties, Map<String, String> map) {
    if (task.getRevision() != null) {
      Revision revision = task.getRevision();
      request.setArguments(revision.getArguments());
      List<String> arguments = revision.getArguments();

      arguments = prepareTemplateTaskArguments(arguments, activityId, applicationProperties, map);

      request.setArguments(arguments);
      if (revision.getImage() != null && !revision.getImage().isBlank()) {
        request.setImage(revision.getImage());
      } else {
        String workerImage =
            this.flowSettinigs.getConfiguration("controller", "worker.image").getValue();
        request.setImage(workerImage);
      }

      List<String> command = revision.getCommand();
      
   
      if (command != null && !command.isEmpty() && !checkForBlankValues(command)) {
        request.setCommand(revision.getCommand());
        List<String> cmdArgs = new LinkedList<>();
        for (String line : revision.getCommand()) {
          String newValue =
              propertyManager.replaceValueWithProperty(line, activityId, applicationProperties);
          cmdArgs.add(newValue);
        }
        
        request.setCommand(cmdArgs);
      }
      if (revision.getScript() != null && !revision.getScript().isBlank()) {
        request.setScript(revision.getScript());
      }
      if (revision.getEnvs() != null) {
        request.setEnvs(revision.getEnvs());
      } else {
        request.setEnvs(new LinkedList<>());
      }
      request.setResults(new LinkedList<>());

      if (task.getResults() != null) {
        request.setResults(task.getResults());
      } else {
        if (revision.getResults() != null) {
          request.setResults(revision.getResults());
        }
      }

    } else {
      taskResult.setStatus(TaskStatus.invalid);
    }
  }

  private boolean checkForBlankValues(List<String> command) {
    for (String str : command) {
      if (str.isBlank()) {
        return true;
      }
    }
    return false;
  }

  private TaskConfiguration buildTaskConfiguration() {
    TaskConfiguration taskConfiguration = new TaskConfiguration();
    TaskDeletion taskDeletion = TaskDeletion.Never;
    String settingsPolicy =
        this.flowSettinigs.getConfiguration("controller", "job.deletion.policy").getValue();
    if (settingsPolicy != null) {
      taskDeletion = TaskDeletion.valueOf(settingsPolicy);
    }
    boolean enableDebug = false;
    String enableDebugFlag =
        this.flowSettinigs.getConfiguration("controller", "enable.debug").getValue();
    if (settingsPolicy != null) {
      enableDebug = Boolean.parseBoolean(enableDebugFlag);
    }

    taskConfiguration.setDeletion(taskDeletion);
    taskConfiguration.setDebug(Boolean.valueOf(enableDebug));

    String taskTimeout =
        this.flowSettinigs.getConfiguration("controller", "task.timeout.configuration").getValue();

    if (taskTimeout != null) {
      int timeout = Integer.parseInt(taskTimeout);
      taskConfiguration.setTimeout(timeout);
    }

    return taskConfiguration;
  }

  private void logPayload(String payloadName, Object request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
      LOGGER.info("Creating For Palyoad Type: {}", payloadName);
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }

  private void logRequestTime(String payloadName, Date start, Date end) {
    long diff = end.getTime() - start.getTime();
    LOGGER.debug("Benchmark [Request Type]: {} - {} ms", payloadName, diff);
  }

  private Map<String, String> convertToMap(List<KeyValuePair> labelList) {
    if (labelList == null) {
      return null;
    }

    Map<String, String> labels = new HashMap<>();
    for (KeyValuePair property : labelList) {
      labels.put(property.getKey(), property.getValue());
    }
    return labels;
  }

  @Override
  public void terminateTask(Task task) {
    TaskExecutionEntity taskExecution = taskService.findById(task.getTaskActivityId());
    ActivityEntity activity =
        this.activityService.findWorkflowActivity(taskExecution.getActivityId());
    try {

      Date startTime = new Date();
      final TaskTemplate request = new TaskTemplate();
      request.setTaskId(task.getTaskId());
      request.setWorkflowId(task.getWorkflowId());
      request.setWorkflowName(task.getWorkflowName());
      request.setWorkflowActivityId(activity.getId());
      request.setTaskName(task.getTaskName());
      request.setTaskActivityId(task.getTaskActivityId());
      logPayload(TERMINATETASKREQUEST, request);

      restTemplate.postForObject(terminateTaskURL, request, TaskResponse.class);

      Date endTime = new Date();
      logRequestTime(TERMINATETASKREQUEST, startTime, endTime);
    } catch (RestClientException ex) {
      LOGGER.error(ERRORLOGPRFIX, TERMINATETASKREQUEST);
      LOGGER.error(ExceptionUtils.getStackTrace(ex));
    }

  }
}
