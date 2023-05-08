package io.boomerang.v4.model;

import java.util.List;

public class HeaderNavigationResponse {
  private HeaderPlatform platform;
  private HeaderFeatures features;
  private List<HeaderNavigation> navigation;
  private List<HeaderOption> featuredServices;
  private HeaderPlatformMessage platformMessage;

  public HeaderPlatform getPlatform() {
    return platform;
  }

  public void setPlatform(HeaderPlatform platform) {
    this.platform = platform;
  }

  public HeaderFeatures getFeatures() {
    return features;
  }

  public void setFeatures(HeaderFeatures features) {
    this.features = features;
  }

  public List<HeaderNavigation> getNavigation() {
    return navigation;
  }

  public void setNavigation(List<HeaderNavigation> navigation) {
    this.navigation = navigation;
  }

  public List<HeaderOption> getFeaturedServices() {
    return featuredServices;
  }

  public void setFeaturedServices(List<HeaderOption> featuredServices) {
    this.featuredServices = featuredServices;
  }

  public HeaderPlatformMessage getPlatformMessage() {
    return platformMessage;
  }

  public void setPlatformMessage(HeaderPlatformMessage platformMessage) {
    this.platformMessage = platformMessage;
  }

}
