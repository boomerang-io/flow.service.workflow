package io.boomerang.data.model;

// Future extensibility
public class UserSettings {

  private Boolean isFirstVisit = true;
  private Boolean isShowHelp = true;
  private Boolean hasConsented = false;

  public Boolean getIsFirstVisit() {
    return isFirstVisit;
  }
  public void setIsFirstVisit(Boolean isFirstVisit) {
    this.isFirstVisit = isFirstVisit;
  }
  public Boolean getIsShowHelp() {
    return isShowHelp;
  }
  public void setIsShowHelp(Boolean isShowHelp) {
    this.isShowHelp = isShowHelp;
  }
  public Boolean getHasConsented() {
    return hasConsented;
  }
  public void setHasConsented(Boolean hasConsented) {
    this.hasConsented = hasConsented;
  }
}
