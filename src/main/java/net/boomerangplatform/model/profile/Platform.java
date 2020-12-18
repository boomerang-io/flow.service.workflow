package net.boomerangplatform.model.profile;

public class Platform {

  private String version;
  private String name;
  private String signOutUrl;
  private String communityUrl;
  private String platformName;
  private Boolean displayLogo;
  private Boolean privateTeams;
  private Boolean sendMail;

  private String baseServicesUrl;
  private String baseEnvUrl;
  
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSignOutUrl() {
    return signOutUrl;
  }

  public void setSignOutUrl(String signOutUrl) {
    this.signOutUrl = signOutUrl;
  }

  public String getCommunityUrl() {
    return communityUrl;
  }

  public void setCommunityUrl(String communityUrl) {
    this.communityUrl = communityUrl;
  }

  public String getPlatformName() {
    return platformName;
  }

  public void setPlatformName(String platformName) {
    this.platformName = platformName;
  }

  public Boolean getDisplayLogo() {
    return displayLogo;
  }

  public void setDisplayLogo(Boolean displayLogo) {
    this.displayLogo = displayLogo;
  }

  public Boolean getPrivateTeams() {
    return privateTeams;
  }

  public void setPrivateTeams(Boolean privateTeams) {
    this.privateTeams = privateTeams;
  }

  public Boolean getSendMail() {
    return sendMail;
  }

  public void setSendMail(Boolean sendMail) {
    this.sendMail = sendMail;
  }

  public String getBaseServicesUrl() {
    return baseServicesUrl;
  }

  public void setBaseServicesUrl(String baseServicesUrl) {
    this.baseServicesUrl = baseServicesUrl;
  }

  public String getBaseEnvUrl() {
    return baseEnvUrl;
  }

  public void setBaseEnvUrl(String baseEnvUrl) {
    this.baseEnvUrl = baseEnvUrl;
  }

}
