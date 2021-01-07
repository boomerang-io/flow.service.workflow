package net.boomerangplatform.service;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.FlowTaskTemplate;
import net.boomerangplatform.model.Task;
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
import net.boomerangplatform.mongo.model.next.DAGTask;
import net.boomerangplatform.mongo.service.ActivityTaskService;
import net.boomerangplatform.mongo.service.FlowGlobalConfigService;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.mongo.service.FlowTeamService;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.crud.TaskTemplateService;
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
  private TaskTemplateService templateService;
  
  @Autowired
  private FlowTaskTemplateService flowTaskTemplateService;


  @Value("${flow.feature.team.properties}")
  private boolean enabledTeamProperites;

  final String[] reserved = {"system", "workflow", "global", "team", "workflow"};

  @Override
  public ControllerRequestProperties buildRequestPropertyLayering(Task task, String activityId) {
    ControllerRequestProperties applicationProperties = new ControllerRequestProperties();
    Map<String, Object> systemProperties = applicationProperties.getSystemProperties();
    Map<String, Object> globalProperties = applicationProperties.getGlobalProperties();
    Map<String, Object> teamProperties = applicationProperties.getTeamProperties();
    Map<String, Object> workflowProperties = applicationProperties.getWorkflowProperties();

    buildGlobalProperties(globalProperties);
    buildSystemProperties(task, activityId, task.getWorkflowId(), systemProperties);

    if (enabledTeamProperites) {
      buildTeamProperties(teamProperties, task.getWorkflowId());
    }
    buildWorkflowProperties(workflowProperties, activityId, null);
    buildTaskInputProperties(applicationProperties, task, activityId);

    return applicationProperties;
  }

  private void buildTaskInputProperties(ControllerRequestProperties applicationProperties,
      Task task, String activityId) {
    ActivityEntity activity = activityService.findWorkflowActivity(activityId);
    Optional<RevisionEntity> revisionOptional =
        revisionService.getRevision(activity.getWorkflowRevisionid());

    Map<String, String> workflowInputProperties = applicationProperties.getTaskInputProperties();
    final Map<String, String> map = task.getInputs();
    if (task.getInputs() != null) {
      for (final Map.Entry<String, String> pair : map.entrySet()) {
        String key = pair.getKey();
        String value = pair.getValue();

        if (value == null || value.isBlank()) {
          value = getDefaultValue(task, revisionOptional, value, key);
        }

        String newValue = this.replaceValueWithProperty(value, activityId, applicationProperties);
        workflowInputProperties.put(key, newValue);
      }
    }
  }

  private String getDefaultValue(Task task, Optional<RevisionEntity> revisionOptional, String value,
      String key) {
    if (value == null || value.isBlank()) {
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
            FlowTaskTemplateEntity taskTemplate = flowTaskTemplateService.getTaskTemplateWithId(templateId);
          
            if (taskTemplate != null) {
              List<Revision> revisions = taskTemplate.getRevisions();
              if (revisions != null) {
                Revision rev = revisions.stream()
                    .filter(e -> e.getVersion().equals(templateVersion)).findFirst().orElse(null);
                if (rev != null) {
                  List<TaskTemplateConfig> configs = rev.getConfig();
                  TaskTemplateConfig taskTemnplate =
                      configs.stream().filter(e -> e.getKey().equals(key)).findAny().orElse(null);
                  if (taskTemnplate != null) {
                    return taskTemnplate.getDefaultValue();
                  }
                }
              }
            }

          }
        }
      }
    }
    return "";
  }

  public void buildWorkflowProperties(Map<String, Object> workflowProperties, String activityId,
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

  public void buildGlobalProperties(Map<String, Object> globalProperties) {
    List<FlowGlobalConfigEntity> globalConfigs = this.flowGlobalConfigService.getGlobalConfigs();
    for (FlowGlobalConfigEntity entity : globalConfigs) {
      if (entity.getValue() != null) {
        globalProperties.put(entity.getKey(), entity.getValue());
      }
    }
  }

  public void buildSystemProperties(Task task, String activityId, String workflowId,
      Map<String, Object> systemProperties) {

    WorkflowEntity workflow = workflowService.getWorkflow(workflowId);
    if (activityId != null) {
      ActivityEntity activity = activityService.findWorkflowActivity(activityId);
      systemProperties.put("workflow-version",
          revisionService.getWorkflowlWithId(activity.getWorkflowRevisionid()).getVersion());
      systemProperties.put("trigger-type", activity.getTrigger());
      systemProperties.put("workflow-activity-initiator", activity.getInitiatedByUserId());
    }

    systemProperties.put("workflow-name", workflow.getName());
    systemProperties.put("workflow-activity-id", activityId);
    systemProperties.put("workflow-id", workflow.getId());

    if (task != null) {
      systemProperties.put("task-name", task.getTaskName());
      systemProperties.put("task-type", task.getTaskType());
    }
  }

  public void buildTeamProperties(Map<String, Object> teamProperties, String workflowId) {
    FlowTeamEntity flowTeamEntity =
        this.flowTeamService.findById(workflowService.getWorkflow(workflowId).getFlowTeamId());
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

    Map<String, String> executionProperties = applicationProperties.getMap();

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
        String params = components[0];
        if ("params".equals(params)) {
          String propertyName = components[1];
          if (executionProperties.get(propertyName) != null) {
            replaceValue = executionProperties.get(propertyName);
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

    Map<String, String> executionProperties = applicationProperties.getMap();

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
}
