package io.boomerang.integrations.model;

public class GHInstallationsResponse {

  private Integer appId;

  private Integer installationId;

  private String orgSlug;
  
  private String orgUrl;
  
  private Integer orgId;
  
  private String orgType;

  public GHInstallationsResponse() {
  }  

  public GHInstallationsResponse(Integer appId, Integer installationId, String orgSlug,
      String orgUrl, Integer orgId, String orgType) {
    this.appId = appId;
    this.installationId = installationId;
    this.orgSlug = orgSlug;
    this.orgUrl = orgUrl;
    this.orgId = orgId;
    this.orgType = orgType;
  }

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
}
