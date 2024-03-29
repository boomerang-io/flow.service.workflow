package io.boomerang.model;

import java.util.Date;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.enums.ref.WorkflowStatus;
import io.boomerang.model.ref.Workflow;

/*
 * Workflow Summary copies from Workflow to include in the Team response
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_EMPTY)
public class WorkflowSummary {
  
  private String id;
  private String name;
  private WorkflowStatus status = WorkflowStatus.active;
  private Integer version = 1;
  private Date creationDate = new Date();
  private String icon;
  private String description;
  
  public WorkflowSummary() {
    
  }
  
  public WorkflowSummary(Workflow workflow) {
    BeanUtils.copyProperties(workflow, this);
  }
  
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
  public Integer getVersion() {
    return version;
  }
  public void setVersion(Integer version) {
    this.version = version;
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
}
