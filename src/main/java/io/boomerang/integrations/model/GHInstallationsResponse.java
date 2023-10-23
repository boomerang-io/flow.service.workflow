package io.boomerang.integrations.model;

import java.util.List;

public class GHInstallationsResponse {

  private Integer appId;

  private Integer installationId;

  private String orgSlug;
  
  private String orgUrl;
  
  private Integer orgId;
  
  private String orgType;
  
  private List<String> events;

  private List<String> repositories;

  public Integer getAppId() {
    return appId;
  }

  public void setAppId(Integer appId) {
    this.appId = appId;
  }

  public Integer getInstallationId() {
    return installationId;
  }

  public void setInstallationId(Integer installationId) {
    this.installationId = installationId;
  }

  public String getOrgSlug() {
    return orgSlug;
  }

  public void setOrgSlug(String orgSlug) {
    this.orgSlug = orgSlug;
  }

  public String getOrgUrl() {
    return orgUrl;
  }

  public void setOrgUrl(String orgUrl) {
    this.orgUrl = orgUrl;
  }

  public Integer getOrgId() {
    return orgId;
  }

  public void setOrgId(Integer orgId) {
    this.orgId = orgId;
  }

  public String getOrgType() {
    return orgType;
  }

  public void setOrgType(String orgType) {
    this.orgType = orgType;
  }

  public List<String> getEvents() {
    return events;
  }

  public void setEvents(List<String> events) {
    this.events = events;
  }

  public List<String> getRepositories() {
    return repositories;
  }

  public void setRepositories(List<String> repositories) {
    this.repositories = repositories;
  }
}
