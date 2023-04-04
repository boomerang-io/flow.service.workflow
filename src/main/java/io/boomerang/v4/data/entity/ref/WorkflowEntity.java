package io.boomerang.v4.data.entity.ref;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.WorkflowToken;
import io.boomerang.v4.model.enums.ref.WorkflowScope;
import io.boomerang.v4.model.ref.WorkflowStatus;
import io.boomerang.v4.model.ref.WorkflowTrigger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows')}")
public class WorkflowEntity {
  
  @Id
  private String id;
  private String name;
  private WorkflowStatus status = WorkflowStatus.active;
  private Date creationDate = new Date();
  private String icon;
  private String description;
  private String shortDescription;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private WorkflowTrigger triggers;
  private List<WorkflowToken> tokens;
  private WorkflowScope scope = WorkflowScope.system;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowStatus getStatus() {
    return status;
  }

  public void setStatus(WorkflowStatus status) {
    this.status = status;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
  }

  public WorkflowTrigger getTriggers() {
    return triggers;
  }

  public void setTriggers(WorkflowTrigger triggers) {
    this.triggers = triggers;
  }

  public WorkflowScope getScope() {
    return scope;
  }

  public void setScope(WorkflowScope scope) {
    this.scope = scope;
  }

  public List<WorkflowToken> getTokens() {
    return tokens;
  }

  public void setTokens(List<WorkflowToken> tokens) {
    this.tokens = tokens;
  }
}
