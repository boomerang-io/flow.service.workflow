package io.boomerang.v4.model.ref;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.boomerang.model.ParamSpec;
import io.boomerang.model.ResultSpec;

public class TaskTemplateSpec {

  private List<String> arguments;
  private List<String> command;
  private List<ParamSpec> params;
  private List<TaskEnvVar> envs;
  private String image;
  private List<ResultSpec> results;
  private String script;
  private String workingDir;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();
  
  public List<String> getArguments() {
    return arguments;
  }

  public List<String> getCommand() {
    return command;
  }

  public List<ParamSpec> getParams() {
    return params;
  }

  public List<TaskEnvVar> getEnvs() {
    return envs;
  }

  public String getImage() {
    return image;
  }

  public List<ResultSpec> getResults() {
    return results;
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

  public void setParams(List<ParamSpec> params) {
    this.params = params;
  }

  public void setEnvs(List<TaskEnvVar> envs) {
    this.envs = envs;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public void setResults(List<ResultSpec> results) {
    this.results = results;
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
}
