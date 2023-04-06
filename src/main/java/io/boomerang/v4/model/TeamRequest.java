package io.boomerang.v4.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.boomerang.v4.data.model.Quotas;

public class TeamRequest {

  private String id;
  private String name;
  private String externalRef;
  private List<UserSummary> users;
  private Quotas quotas;
  private Map<String, String> labels = new HashMap<>();

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

  public String getExternalRef() {
    return externalRef;
  }

  public void setExternalRef(String externalRef) {
    this.externalRef = externalRef;
  }

  public List<UserSummary> getUsers() {
    return users;
  }

  public void setUsers(List<UserSummary> users) {
    this.users = users;
  }

  public Quotas getQuotas() {
    return quotas;
  }

  public void setQuotas(Quotas quotas) {
    this.quotas = quotas;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
}
