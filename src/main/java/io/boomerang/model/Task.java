package io.boomerang.model;

import java.util.List;
import java.util.Map;
import io.boomerang.model.controller.TaskResult;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.TaskType;
import io.boomerang.mongo.model.next.Dependency;

public class Task {

  private TaskType taskType;
  private List<String> dependencies;

  private List<Dependency> detailedDepednacies;

  private Map<String, String> inputs;


  private String taskId;
  private String taskName;
  private String templateId;


  
  private String workflowId;
  private String workflowName;
  private String taskActivityId;
  private boolean enableLifecycle;

  private String decisionValue;

  private Revision revision;

  private List<TaskResult> results;
  
  public List<TaskResult> getResults() {
    return results;
  }

  public void setResults(List<TaskResult> results) {
    this.results = results;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public String getWorkflowName() {
    return workflowName;
  }


  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }

  public void setTaskId(String taskName) {
    this.taskId = taskName;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType nodeType) {
    this.taskType = nodeType;
  }

  public Map<String, String> getInputs() {
    return inputs;
  }

  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
  }

  public List<Dependency> getDetailedDepednacies() {
    return detailedDepednacies;
  }

  public void setDetailedDepednacies(List<Dependency> detailedDepednacies) {
    this.detailedDepednacies = detailedDepednacies;
  }

  public String getDecisionValue() {
    return decisionValue;
  }

  public void setDecisionValue(String decisionValue) {
    this.decisionValue = decisionValue;
  }

  public String getTaskActivityId() {
    return taskActivityId;
  }

  public void setTaskActivityId(String taskActivityId) {
    this.taskActivityId = taskActivityId;
  }

  public Revision getRevision() {
    return revision;
  }

  public void setRevision(Revision revision) {
    this.revision = revision;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  public boolean getEnableLifecycle() {
    return enableLifecycle;
  }

  public void setEnableLifecycle(boolean enableLifecycle) {
    this.enableLifecycle = enableLifecycle;
  }

}
