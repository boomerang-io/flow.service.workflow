package io.boomerang.v4.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.ApproverGroupEntity;

public class ApproverGroup {

  private String id;
  private String name;
  private Date creationDate = new Date();
  private List<TeamMember> approvers = new LinkedList<>();  

  public ApproverGroup() {
    
  }
  
  public ApproverGroup(ApproverGroupEntity entity) {
    BeanUtils.copyProperties(entity, this, "approvers");
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

  public List<TeamMember> getApprovers() {
    return approvers;
  }

  public void setApprovers(List<TeamMember> approvers) {
    this.approvers = approvers;
  }
}
