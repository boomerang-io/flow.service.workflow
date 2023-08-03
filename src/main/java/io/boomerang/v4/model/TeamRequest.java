package io.boomerang.v4.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.model.enums.TeamStatus;

public class TeamRequest {

  private String id;
  private String name;
  private TeamStatus status;
  private String externalRef;
  private Map<String, String> labels = new HashMap<>();
  private List<AbstractParam> parameters = new LinkedList<>();
  private Quotas quotas;
  private List<TeamMember> members;
  private List<ApproverGroupRequest> approverGroups;
  
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
  public List<TeamMember> getMembers() {
    return members;
  }
  public void setMembers(List<TeamMember> members) {
    this.members = members;
  }
  public List<ApproverGroupRequest> getApproverGroups() {
    return approverGroups;
  }
  public void setApproverGroups(List<ApproverGroupRequest> approverGroups) {
    this.approverGroups = approverGroups;
  }
}
