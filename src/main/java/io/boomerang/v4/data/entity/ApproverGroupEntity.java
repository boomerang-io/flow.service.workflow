package io.boomerang.v4.data.entity;

import java.util.Date;
import java.util.List;

public class ApproverGroupEntity {

  private String id;
  private String name;
  private Date creationDate = new Date();
  private List<String> approverRefs;

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
  
  public List<String> getApproverRefs() {
    return approverRefs;
  }
  
  public void setApproverRefs(List<String> approverRefs) {
    this.approverRefs = approverRefs;
  }
}
