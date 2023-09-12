package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.model.Quotas;
import io.boomerang.data.model.TeamSettings;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.enums.TeamStatus;
import io.boomerang.model.enums.TeamType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('teams')}")
public class TeamEntity {

  @Id
  private String id;
  private String name;
  private Date creationDate = new Date();
  private TeamType type = TeamType.team; 
  private TeamStatus status = TeamStatus.active;
  private String externalRef;
  private Map<String, String> labels = new HashMap<>();
  private List<AbstractParam> parameters = new LinkedList<>();
//  private TeamSettings settings;
  private Quotas quotas;
  
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
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
  public TeamType getType() {
    return type;
  }
  public void setType(TeamType type) {
    this.type = type;
  }
  public TeamStatus getStatus() {
    return status;
  }
  public void setStatus(TeamStatus status) {
    this.status = status;
  }
  public String getExternalRef() {
    return externalRef;
  }
  public void setExternalRef(String externalRef) {
    this.externalRef = externalRef;
  }
//  public TeamSettings getSettings() {
//    return settings;
//  }
//  public void setSettings(TeamSettings settings) {
//    this.settings = settings;
//  }
  public Map<String, String> getLabels() {
    return labels;
  }
  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
  public List<AbstractParam> getParameters() {
    return parameters;
  }
  public void setParameters(List<AbstractParam> parameters) {
    this.parameters = parameters;
  }
  public Quotas getQuotas() {
    return quotas;
  }
  public void setQuotas(Quotas quotas) {
    this.quotas = quotas;
  }
}
