package io.boomerang.mongo.entity;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.WorkflowToken;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.WorkflowStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows')}")
public class WorkflowEntity {

  private List<WorkflowProperty> properties;

  private String description;

  private String flowTeamId;
  private String ownerUserId;
  
  private String icon;
  @Id
  private String id;

  private String name;

  private String shortDescription;

  private WorkflowStatus status;

  private Triggers triggers;
  
  private List<WorkflowToken> tokens;

  private boolean enablePersistentStorage;
  
  
  private List<KeyValuePair> labels;
  
  private WorkflowScope scope;

  public List<WorkflowToken> getTokens() {
    return tokens;
  }

  public void setTokens(List<WorkflowToken> tokens) {
    this.tokens = tokens;
  }
  
  public String getDescription() {
    return description;
  }

  public String getFlowTeamId() {
    return flowTeamId;
  }

  public String getIcon() {
    return icon;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public WorkflowStatus getStatus() {
    return status;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setFlowTeamId(String flowTeamId) {
    this.flowTeamId = flowTeamId;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setStatus(WorkflowStatus status) {
    this.status = status;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public Triggers getTriggers() {
    return triggers;
  }

  public void setTriggers(Triggers triggers) {
    this.triggers = triggers;
  }

  public boolean isEnablePersistentStorage() {
    return enablePersistentStorage;
  }

  public void setEnablePersistentStorage(boolean enablePersistentStorage) {
    this.enablePersistentStorage = enablePersistentStorage;
  }

  public List<WorkflowProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<WorkflowProperty> properties) {
    this.properties = properties;
  }

  public WorkflowScope getScope() {
    return scope;
  }

  public void setScope(WorkflowScope scope) {
    this.scope = scope;
  }

  public List<KeyValuePair> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValuePair> labels) {
    this.labels = labels;
  }

  public String getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(String ownerUserId) {
    this.ownerUserId = ownerUserId;
  }


}
