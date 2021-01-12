package net.boomerangplatform.model.controller;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "taskType")
@JsonSubTypes({@Type(value = TaskCustom.class, name = "custom"),
    @Type(value = TaskTemplate.class, name = "template")})
@JsonIgnoreProperties
public abstract class Task {

  private String workflowName;

  private String workflowId;

  private String workflowActivityId;

  private String taskActivityId;

  private String taskName;

  private String taskId;

  @JsonProperty("image")
  private String image;

  @JsonProperty("command")
  private String command;

  @JsonProperty("parameters")
  private Map<String, String> parameters = new HashMap<>();

  @JsonProperty("arguments")
  private List<String> arguments;

  @JsonProperty("configuration")
  private TaskConfiguration configuration;

  @JsonProperty("workspaces")
  private List<TaskWorkspace> workspaces;

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowActivityId() {
    return workflowActivityId;
  }

  public void setWorkflowActivityId(String workflowActivityId) {
    this.workflowActivityId = workflowActivityId;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public Map<String, String> getParameters() {
    return this.parameters;
  }

  public void setParameter(String name, String value) {
    this.parameters.put(name, value);
  }

  public List<String> getArguments() {
    return arguments;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  public void setArgument(String argument) {
    this.arguments.add(argument);
  }

  public String getTaskActivityId() {
    return taskActivityId;
  }

  public void setTaskActivityId(String taskActivityId) {
    this.taskActivityId = taskActivityId;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public TaskConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(TaskConfiguration configuration) {
    this.configuration = configuration;
  }

  public List<TaskWorkspace> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<TaskWorkspace> workspaces) {
    this.workspaces = workspaces;
  }
}
