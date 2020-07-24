package net.boomerangplatform.mongo.model;

import java.util.List;

public class Revision {

  private Integer version;
  private String image;
  private String command;

  private List<String> arguments;
  private List<TaskTemplateConfig> config;

  private ChangeLog changelog;

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
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

  public List<String> getArguments() {
    return arguments;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments;
  }

  public List<TaskTemplateConfig> getConfig() {
    return config;
  }

  public void setConfig(List<TaskTemplateConfig> config) {
    this.config = config;
  }

  public ChangeLog getChangelog() {
    return changelog;
  }

  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
  }
}
