package net.boomerangplatform.mongo.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import net.boomerangplatform.mongo.model.FlowTaskTemplateStatus;
import net.boomerangplatform.mongo.model.Quotas;
import net.boomerangplatform.mongo.model.Revision;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "flow_task_templates")
public class FlowTaskTemplateEntity {

  @Id
  private String id;

  private String description;

  private Date lastModified;
  private String name;
  private String category;
  private String nodetype;
  private List<Revision> revisions;
  private FlowTaskTemplateStatus status;
  private Date createdDate;
  private String icon;
  private Quotas quotas;

  public FlowTaskTemplateEntity() {
    // Do nothing
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getNodeType() {
    return nodetype;
  }

  public void setNodeType(String nodetype) {
    this.nodetype = nodetype;
  }

  public List<Revision> getRevisions() {
    return revisions;
  }

  public void setRevisions(List<Revision> revisions) {
    this.revisions = revisions;
  }

  public FlowTaskTemplateStatus getStatus() {
    return status;
  }

  public void setStatus(FlowTaskTemplateStatus status) {
    this.status = status;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

}
