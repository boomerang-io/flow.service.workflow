package io.boomerang.v4.data.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import io.boomerang.mongo.model.ConfigurationType;
import io.boomerang.v4.model.AbstractParam;

@Document(collection = "#{@mongoConfiguration.fullCollectionName('settings')}")
public class SettingEntity {

  private List<AbstractParam> config;

  private String description;
  @Id
  private String id;
  private String key;

  private Date lastModiifed;

  private String name;

  private String tool;

  private ConfigurationType type;

  public List<AbstractParam> getConfig() {
    return config;
  }

  public String getDescription() {
    return description;
  }

  public String getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public Date getLastModiifed() {
    return lastModiifed;
  }

  public String getName() {
    return name;
  }

  public String getTool() {
    return tool;
  }

  public ConfigurationType getType() {
    return type;
  }

  public void setConfig(List<AbstractParam> config) {
    this.config = config;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void setLastModiifed(Date lastModiifed) {
    this.lastModiifed = lastModiifed;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setTool(String tool) {
    this.tool = tool;
  }

  public void setType(ConfigurationType type) {
    this.type = type;
  }

}
