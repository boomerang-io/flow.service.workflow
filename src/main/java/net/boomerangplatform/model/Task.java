package net.boomerangplatform.model;

import java.util.List;
import java.util.Map;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.TaskType;
import net.boomerangplatform.mongo.model.next.Dependency;

public class Task {

  private TaskType taskType;
  private List<String> dependencies;

  private List<Dependency> detailedDepednacies;

  private Map<String, String> inputs;

  private String taskId;
  private String taskName;

  private String workflowId;
  private String workflowName;
  private String taskActivityId;

  private String decisionValue;

  private Revision revision;


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

}
