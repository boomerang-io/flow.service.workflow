package io.boomerang.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.boomerang.model.Task;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.WorkflowToken;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.FlowGlobalConfigEntity;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.FlowTeamConfiguration;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskTemplateConfig;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.ActivityTaskService;
import io.boomerang.mongo.service.FlowGlobalConfigService;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowTeamService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.ControllerRequestProperties;
import io.boomerang.mongo.service.FlowSettingsService;

@Service
public class PropertyManagerImpl implements PropertyManager {

  @Autowired
  private FlowSettingsService flowSettingsService;

  @Autowired
  private RevisionService revisionService;

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private FlowTeamService flowTeamService;

  @Autowired
  private FlowActivityService activityService;

  @Autowired
  public ActivityTaskService taskService;

  @Autowired
  private FlowGlobalConfigService flowGlobalConfigService;

  @Autowired
  private FlowTaskTemplateService flowTaskTemplateService;

  @Value("${flow.services.listener.webhook.url}")
  private String webhookUrl;


  @Value("${flow.services.listener.wfe.url}")
  private String waitForEventUrl;


  @Value("${flow.services.listener.event.url}")
  private String eventUrl;


  final String[] reserved = {"system", "workflow", "global", "team", "workflow"};

  @Override
  public ControllerRequestProperties buildRequestPropertyLayering(Task task, String activityId,
      String workflowId) {
    ControllerRequestProperties applicationProperties = new ControllerRequestProperties();
    Map<String, String> systemProperties = applicationProperties.getSystemProperties();
    Map<String, String> globalProperties = applicationProperties.getGlobalProperties();
    Map<String, String> teamProperties = applicationProperties.getTeamProperties();
    Map<String, String> workflowProperties = applicationProperties.getWorkflowProperties();
    Map<String, String> reservedProperties = applicationProperties.getReservedProperties();

    buildGlobalProperties(globalProperties);
    buildSystemProperties(task, activityId, workflowId, systemProperties);
    buildReservedPropertyList(reservedProperties, workflowId);

    if (flowSettingsService.getConfiguration("features", "teamParameters").getBooleanValue()) {
      buildTeamProperties(teamProperties, workflowId);
    }
    buildWorkflowProperties(workflowProperties, activityId, workflowId);

    if (task != null) {
      buildTaskInputProperties(applicationProperties, task, activityId);
    }


    return applicationProperties;
  }

  private void buildReservedPropertyList(Map<String, String> reservedProperties,
      String workflowId) {

    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
    if (workflow.getTokens() != null) {
      for (WorkflowToken token : workflow.getTokens()) {
        reservedProperties.put("system.tokens." + token.getLabel(), token.getToken());
      }
    }
  }

  private void buildTaskInputProperties(ControllerRequestProperties applicationProperties,
      Task task, String activityId) {
    ActivityEntity activity = activityService.findWorkflowActivity(activityId);

    List<TaskTemplateConfig> configs = getInputsForTask(task, activity.getWorkflowRevisionid());

    Map<String, String> workflowInputProperties = applicationProperties.getTaskInputProperties();
    for (TaskTemplateConfig config : configs) {
      String key = config.getKey();
      String value = this.getInputForTaskKey(task, activity.getWorkflowRevisionid(), key);

      if (value == null || value.isBlank()) {
        value = config.getDefaultValue();
      }

      if (value != null) {
        String newValue = this.replaceValueWithProperty(value, activityId, applicationProperties);
        newValue = this.replaceValueWithProperty(newValue, activityId, applicationProperties);

        newValue = this.replaceAllParams(newValue, activityId, applicationProperties);

        workflowInputProperties.put(key, newValue);
      } else {
        workflowInputProperties.put(key, "");
      }
    }
  }

  private String getInputForTaskKey(Task task, String revisionId, String key) {
    Optional<RevisionEntity> revisionOptional = revisionService.getRevision(revisionId);
    if (revisionOptional.isPresent()) {
      RevisionEntity revision = revisionOptional.get();
      Dag dag = revision.getDag();
      List<DAGTask> tasks = dag.getTasks();
      if (tasks != null) {
        DAGTask dagTask = tasks.stream().filter(e -> e.getTaskId().equals(task.getTaskId()))
            .findFirst().orElse(null);
        if (dagTask != null) {
          List<KeyValuePair> properties = dagTask.getProperties();
          if (properties != null) {
            KeyValuePair property =
                properties.stream().filter(e -> key.equals(e.getKey())).findFirst().orElse(null);
            if (property != null) {
              return property.getValue();
            }
          }
        }
      }
    }
    return null;
  }


  private List<TaskTemplateConfig> getInputsForTask(Task task, String revisionId) {
    Optional<RevisionEntity> revisionOptional = revisionService.getRevision(revisionId);
    if (revisionOptional.isPresent()) {
      RevisionEntity revision = revisionOptional.get();
      Dag dag = revision.getDag();
      List<DAGTask> tasks = dag.getTasks();
      if (tasks != null) {
        DAGTask dagTask = tasks.stream().filter(e -> e.getTaskId().equals(task.getTaskId()))
            .findFirst().orElse(null);
        if (dagTask != null) {
          String templateId = dagTask.getTemplateId();
          Integer templateVersion = dagTask.getTemplateVersion();
          FlowTaskTemplateEntity taskTemplate =
              flowTaskTemplateService.getTaskTemplateWithId(templateId);

          if (taskTemplate != null) {
            List<Revision> revisions = taskTemplate.getRevisions();
            if (revisions != null) {
              Revision rev = revisions.stream().filter(e -> e.getVersion().equals(templateVersion))
                  .findFirst().orElse(null);
              if (rev != null && rev.getConfig() != null) {
                return rev.getConfig();
              }
            }
          }
        }
      }
    }
    return new LinkedList<>();
  }


  @Override
  public void buildWorkflowProperties(Map<String, String> workflowProperties, String activityId,
      String workflowId) {
    WorkflowEntity workflow = new WorkflowEntity();
    List<KeyValuePair> properties = null;
    if (activityId != null) {
      ActivityEntity activity = activityService.findWorkflowActivity(activityId);
      workflow = workflowService.getWorkflow(activity.getWorkflowId());
      properties = activity.getProperties();
    } else {
      workflow = workflowService.getWorkflow(workflowId);
    }

    if (workflow.getProperties() != null) {
      for (WorkflowProperty property : workflow.getProperties()) {
        workflowProperties.put(property.getKey(), property.getDefaultValue());
      }
    }

    if (properties != null) {
      for (KeyValuePair property : properties) {
        workflowProperties.put(property.getKey(), property.getValue());
      }
    }
  }

  @Override
  public void buildGlobalProperties(Map<String, String> globalProperties) {
    List<FlowGlobalConfigEntity> globalConfigs = this.flowGlobalConfigService.getGlobalConfigs();
    for (FlowGlobalConfigEntity entity : globalConfigs) {
      if (entity.getValue() != null) {
        globalProperties.put(entity.getKey(), entity.getValue());
      }
    }
  }

  @Override
  public void buildSystemProperties(Task task, String activityId, String workflowId,
      Map<String, String> systemProperties) {

    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
    if (activityId != null) {
      ActivityEntity activity = activityService.findWorkflowActivity(activityId);
      RevisionEntity revision =
          revisionService.getWorkflowlWithId(activity.getWorkflowRevisionid());

      if (revision != null) {
        systemProperties.put("workflow-version", Long.toString(revision.getVersion()));
      }
      systemProperties.put("trigger-type", activity.getTrigger());
      systemProperties.put("workflow-activity-initiator", "");
      if (activity.getInitiatedByUserId() != null) {
        systemProperties.put("workflow-activity-initiator", activity.getInitiatedByUserId());
      }
    }

    systemProperties.put("workflow-name", workflow.getName());
    systemProperties.put("workflow-activity-id", activityId);
    systemProperties.put("workflow-id", workflow.getId());

    systemProperties.put("trigger-webhook-url", this.webhookUrl);
    systemProperties.put("trigger-wfe-url", this.waitForEventUrl);
    systemProperties.put("trigger-event-url", this.eventUrl);


    if (task != null) {
      systemProperties.put("task-name", task.getTaskName());
      systemProperties.put("task-id", task.getTaskId());
      systemProperties.put("task-type", task.getTaskType().toString());
    }
  }

  @Override
  public void buildTeamProperties(Map<String, String> teamProperties, String workflowId) {
    WorkflowSummary workflow = workflowService.getWorkflow(workflowId);

    if (WorkflowScope.team.equals(workflow.getScope())) {
      TeamEntity flowTeamEntity = this.flowTeamService.findById(workflow.getFlowTeamId());
      if (flowTeamEntity == null) {
        return;
      }

      List<FlowTeamConfiguration> teamConfig = null;
      if (flowTeamEntity.getSettings() != null) {
        teamConfig = flowTeamEntity.getSettings().getProperties();
      }
      if (teamConfig != null) {
        for (FlowTeamConfiguration config : teamConfig) {
          teamProperties.put(config.getKey(), config.getValue());
        }
      }
    }
  }

  @Override
  public String replaceValueWithProperty(String value, String activityId,
      ControllerRequestProperties properties) {

    String replacementString = value;
    replacementString = replaceProperties(replacementString, activityId, properties);

    return replacementString;
  }

  private String replaceProperties(String value, String activityId,
      ControllerRequestProperties applicationProperties) {

    Map<String, String> executionProperties = applicationProperties.getMap(true);

    String regex = "(?<=\\$\\().+?(?=\\))";
    Pattern pattern = Pattern.compile(regex);
    Matcher m = pattern.matcher(value);
    List<String> originalValues = new LinkedList<>();
    List<String> newValues = new LinkedList<>();
    while (m.find()) {
      String extractedValue = m.group(0);
      String replaceValue = null;

      int start = m.start() - 2;
      int end = m.end() + 1;
      String[] components = extractedValue.split("\\.");

      if (components.length == 2) {
        List<String> reservedList = Arrays.asList(reserved);

        String params = components[0];
        if ("params".equals(params)) {

          String propertyName = components[1];


          if (executionProperties.get(propertyName) != null) {
            replaceValue = executionProperties.get(propertyName);
          } else {
            replaceValue = "";
          }
        } else if (reservedList.contains(params)) {
          String key = components[1];
          if ("allParams".equals(key)) {
            Map<String, String> properties = applicationProperties.getMapForKey(params);
            replaceValue = this.getEncodedPropertiesForMap(properties);
          }
        }
      } else if (components.length == 4) {

        String task = components[0];
        String taskName = components[1];
        String results = components[2];
        String outputProperty = components[3];

        if (("task".equals(task) || "tasks".equals(task)) && "results".equals(results)) {

          TaskExecutionEntity taskExecution = getTaskExecutionEntity(activityId, taskName);
          if (taskExecution != null && taskExecution.getOutputs() != null
              && taskExecution.getOutputs().get(outputProperty) != null) {
            replaceValue = taskExecution.getOutputs().get(outputProperty);
          } else {
            replaceValue = "";
          }
        }
      } else if (components.length == 3) {
        String scope = components[0];
        String params = components[1];
        String name = components[2];
        List<String> reservedList = Arrays.asList(reserved);
        if ("tokens".equals(params) && "system".equals(scope)) {
          if (executionProperties.get(extractedValue) != null) {
            replaceValue = executionProperties.get(extractedValue);
          } else {
            replaceValue = "";
          }
        } else if ("params".equals(params) && reservedList.contains(scope)) {
          if (reservedList.contains(scope)) {
            String key = scope + "/" + name;

            if (executionProperties.get(key) != null) {
              replaceValue = executionProperties.get(key);
            } else {
              replaceValue = "";
            }
          }
        }
      }

      if (replaceValue != null) {
        String regexStr = value.substring(start, end);
        originalValues.add(regexStr);
        newValues.add(replaceValue);
      }
    }

    String[] originalValuesArray = originalValues.toArray(new String[originalValues.size()]);
    String[] newValuesArray = newValues.toArray(new String[newValues.size()]);
    String updatedString = StringUtils.replaceEach(value, originalValuesArray, newValuesArray);
    return updatedString;
  }

  private String replaceAllParams(String value, String activityId,
      ControllerRequestProperties applicationProperties) {

    String regex = "(?<=\\$\\().+?(?=\\))";
    Pattern pattern = Pattern.compile(regex);
    Matcher m = pattern.matcher(value);
    List<String> originalValues = new LinkedList<>();
    List<String> newValues = new LinkedList<>();
    while (m.find()) {
      String extractedValue = m.group(0);
      String replaceValue = null;

      int start = m.start() - 2;
      int end = m.end() + 1;
      String[] components = extractedValue.split("\\.");

      if (components.length == 1) {
        String allParams = components[0];
        if ("allParams".equals(allParams)) {
          Map<String, String> properties = applicationProperties.getMap(false);
          for (Map.Entry<String, String> entry : properties.entrySet()) {
            String originalValue = entry.getValue();
            String newValue =
                this.replaceValueWithProperty(originalValue, activityId, applicationProperties);
            newValue = this.replaceValueWithProperty(newValue, activityId, applicationProperties);
            entry.setValue(newValue);
          }
          replaceValue = this.getEncodedPropertiesForMap(properties);
        }
      }

      if (replaceValue != null) {
        String regexStr = value.substring(start, end);
        originalValues.add(regexStr);
        newValues.add(replaceValue);
      }
    }

    String[] originalValuesArray = originalValues.toArray(new String[originalValues.size()]);
    String[] newValuesArray = newValues.toArray(new String[newValues.size()]);
    String updatedString = StringUtils.replaceEach(value, originalValuesArray, newValuesArray);
    return updatedString;
  }



  private TaskExecutionEntity getTaskExecutionEntity(String activityId, String taskName) {

    List<TaskExecutionEntity> tasks = taskService.findTaskActiivtyForActivity(activityId);
    for (TaskExecutionEntity task : tasks) {
      String entityTaskName = task.getTaskName().toLowerCase().replaceAll("\\s+", "");
      String sanataizedTaskName = taskName.toLowerCase().replaceAll("\\s+", "");

      if (entityTaskName.equals(sanataizedTaskName)) {
        return task;
      }
    }
    return null;
  }


  private String getEncodedPropertiesForMap(Map<String, String> map) {
    Properties properties = new Properties();

    for (Map.Entry<String, String> entry : map.entrySet()) {
      String originalKey = entry.getKey();
      String value = entry.getValue();
      String modifiedKey = originalKey.replaceAll("-", "\\.");
      properties.put(modifiedKey, value);
    }

    try {
      properties.putAll(map);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      properties.store(outputStream, null);
      String text = outputStream.toString();
      String[] lines = text.split("\\n");

      StringBuilder sb = new StringBuilder();
      for (String line : lines) {
        if (!line.startsWith("#")) {
          sb.append(line + '\n');
        }

      }
      String propertiesFile = sb.toString();
      String encodedString = Base64.getEncoder().encodeToString(propertiesFile.getBytes());
      return encodedString;
    } catch (IOException e) {
      return "";
    }
  }

}
