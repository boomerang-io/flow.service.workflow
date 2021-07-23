package net.boomerangplatform.mongo.model;

import java.util.List;
import net.boomerangplatform.model.controller.TaskEnvVar;
import net.boomerangplatform.model.controller.TaskResult;

public class Revision {

  private List<String> arguments;
  private ChangeLog changelog;
  private List<String> command;
  private List<TaskTemplateConfig> config;
  private List<TaskEnvVar> envs;
  
  private String image;
  
  private List<TaskResult> results;

  private String script;

  private Integer version;

  private String workingDir;
  public List<String> getArguments() {
    return arguments;
  }

  public ChangeLog getChangelog() {
    return changelog;
  }

  public List<String> getCommand() {
    return command;
  }

  public List<TaskTemplateConfig> getConfig() {
    return config;
  }

  public List<TaskEnvVar> getEnvs() {
    return envs;
  }

  public String getImage() {
    return image;
  }

  public List<TaskResult> getResults() {
    return results;
  }

  public String getScript() {
    return script;
  }

  public Integer getVersion() {
    return version;
  }

  public String getWorkingDir() {
    return workingDir;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
  }

  public void setCommand(List<String>  command) {
    this.command = command;
  }

  public void setConfig(List<TaskTemplateConfig> config) {
    this.config = config;
  }

  public void setEnvs(List<TaskEnvVar> envs) {
    this.envs = envs;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public void setResults(List<TaskResult> results) {
    this.results = results;
  }

  public void setScript(String script) {
    this.script = script;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setWorkingDir(String workingDir) {
    this.workingDir = workingDir;
  }
}
