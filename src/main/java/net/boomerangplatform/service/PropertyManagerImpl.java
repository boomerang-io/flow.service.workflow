package net.boomerangplatform.service;

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
import net.boomerangplatform.model.Task;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.FlowGlobalConfigEntity;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.FlowTeamConfiguration;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.Dag;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskTemplateConfig;
import net.boomerangplatform.mongo.model.WorkflowScope;
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.FlowGlobalConfigService;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowTeamService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.crud.WorkflowService;
import net.boomerangplatform.service.refactor.ControllerRequestProperties;

@Service
public class PropertyManagerImpl implements PropertyManager {

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

  @Value("${flow.feature.team.parameters}")
  private boolean enabledTeamProperites;

  final String[] reserved = {"system", "workflow", "global", "team", "workflow"};

  @Override
  public ControllerRequestProperties buildRequestPropertyLayering(Task task, String activityId,
      String workflowId) {
    ControllerRequestProperties applicationProperties = new ControllerRequestProperties();
    Map<String, String> systemProperties = applicationProperties.getSystemProperties();
    Map<String, String> globalProperties = applicationProperties.getGlobalProperties();
    Map<String, String> teamProperties = applicationProperties.getTeamProperties();
    Map<String, String> workflowProperties = applicationProperties.getWorkflowProperties();

    buildGlobalProperties(globalProperties);
    buildSystemProperties(task, activityId, workflowId, systemProperties);

    if (enabledTeamProperites) {
      buildTeamProperties(teamProperties, workflowId);
    }
    buildWorkflowProperties(workflowProperties, activityId, workflowId);

    if (task != null) {
      buildTaskInputProperties(applicationProperties, task, activityId);
    }

    return applicationProperties;
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
          List<CoreProperty> properties = dagTask.getProperties();
          if (properties != null) {
            CoreProperty property =
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
              if (rev != null) {
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
    List<CoreProperty> properties = null;
    if (activityId != null) {
      ActivityEntity activity = activityService.findWorkflowActivity(activityId);
      workflow = workflowService.getWorkflow(activity.getWorkflowId());
      properties = activity.getProperties();
    } else {
      workflow = workflowService.getWorkflow(workflowId);
    }

    if (workflow.getProperties() != null) {
      for (FlowProperty property : workflow.getProperties()) {
        workflowProperties.put(property.getKey(), property.getDefaultValue());
      }
    }

    if (properties != null) {
      for (CoreProperty property : properties) {
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
      systemProperties.put("workflow-version", Long.toString(
          revisionService.getWorkflowlWithId(activity.getWorkflowRevisionid()).getVersion()));
      systemProperties.put("trigger-type", activity.getTrigger());

      systemProperties.put("workflow-activity-initiator", "");
      if (activity.getInitiatedByUserId() != null) {
        systemProperties.put("workflow-activity-initiator", activity.getInitiatedByUserId());
      }


    }

    systemProperties.put("workflow-name", workflow.getName());
    systemProperties.put("workflow-activity-id", activityId);
    systemProperties.put("workflow-id", workflow.getId());

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
      FlowTeamEntity flowTeamEntity = this.flowTeamService.findById(workflow.getFlowTeamId());
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
    replacementString = replaceLegacyProperties(value, activityId, properties);
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

      if (components.length == 1) {
        String allParams = components[0];
        if ("allParams".equals(allParams)) {
          Map<String, String> properties = applicationProperties.getMap(false);
          replaceValue = this.getEncodedPropertiesForMap(properties);
        }
      } else if (components.length == 2) {
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
        if ("task".equals(task) && "results".equals(results)) {
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
        if ("params".equals(params) && reservedList.contains(scope)) {
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

  private String replaceLegacyProperties(String value, String activityId,
      ControllerRequestProperties applicationProperties) {

    Map<String, String> executionProperties = applicationProperties.getMap(true);

    String regex = "\\$\\{p\\:([^{}]*)\\}";
    Pattern pattern = Pattern.compile(regex);
    Matcher m = pattern.matcher(value);
    List<String> originalValues = new LinkedList<>();
    List<String> newValues = new LinkedList<>();
    while (m.find()) {
      String extractedValue = m.group(1);
      String replaceValue = null;
      int start = m.start();
      int end = m.end();
      String[] components = extractedValue.split("/");
      if (components.length == 1) {
        if (executionProperties.get(components[0]) != null) {
          replaceValue = executionProperties.get(components[0]);
        }
      } else if (components.length == 2) {
        String scope = components[0];
        List<String> reservedList = Arrays.asList(reserved);
        if (reservedList.contains(scope)) {
          if (executionProperties.get(extractedValue) != null) {
            replaceValue = executionProperties.get(extractedValue);
          }
        } else {
          String taskName = components[0];
          String outputProperty = components[1];
          TaskExecutionEntity taskExecution = getTaskExecutionEntity(activityId, taskName);
          if (taskExecution != null && taskExecution.getOutputs() != null
              && taskExecution.getOutputs().get(outputProperty) != null) {
            replaceValue = taskExecution.getOutputs().get(outputProperty);
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
    if (map.containsKey("version.name")) {
      map.put("version-name", map.get("version.name"));
    }

    if (map.containsKey("docker.image.name")) {
      map.put("docker-image-name", map.get("docker.image.name"));
    }

    try {
      Properties properties = new Properties();
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
