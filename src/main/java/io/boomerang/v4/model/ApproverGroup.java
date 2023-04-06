package io.boomerang.v4.model;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.ApproverGroupEntity;

public class ApproverGroup {

  private String id;
  private String name;
  private Date creationDate = new Date();
  private List<UserSummary> approvers = Collections.emptyList();  

  public ApproverGroup() {
    
  }
  
  public ApproverGroup(ApproverGroupEntity entity) {
    BeanUtils.copyProperties(entity, this, "approverRefs");
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

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public List<UserSummary> getApprovers() {
    return approvers;
  }

  public void setApprovers(List<UserSummary> approvers) {
    this.approvers = approvers;
  }
}
