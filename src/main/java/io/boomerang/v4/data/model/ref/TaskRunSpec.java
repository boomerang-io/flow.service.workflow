package io.boomerang.v4.data.model.ref;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class TaskRunSpec {

  private List<String> arguments;
  private List<String> command;
  private List<TaskEnvVar> envs;
  private String image;
  private String script;
  private String workingDir;
  private Boolean debug = false;
  private int timeout;
  private TaskDeletionEnum deletion;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();
  
  public List<String> getArguments() {
    return arguments;
  }

  public List<String> getCommand() {
    return command;
  }

  public List<TaskEnvVar> getEnvs() {
    return envs;
  }

  public String getImage() {
    return image;
  }

  public String getScript() {
    return script;
  }

  public String getWorkingDir() {
    return workingDir;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  public void setCommand(List<String>  command) {
    this.command = command;
  }
  
  public void setEnvs(List<TaskEnvVar> envs) {
    this.envs = envs;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public void setWorkingDir(String workingDir) {
    this.workingDir = workingDir;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public TaskDeletionEnum getDeletion() {
    return deletion;
  }

  public void setDeletion(TaskDeletionEnum deletion) {
    this.deletion = deletion;
  }
}
