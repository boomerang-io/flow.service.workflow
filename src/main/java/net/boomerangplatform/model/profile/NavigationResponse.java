package net.boomerangplatform.model.profile;

import java.util.List;

public class NavigationResponse {
  private Platform platform;
  private Features features;
  private List<Navigation> navigation;
  private List<Option> featuredServices;
  private PlatformMessage platformMessage;

  public Platform getPlatform() {
    return platform;
  }

  public void setPlatform(Platform platform) {
    this.platform = platform;
  }

  public Features getFeatures() {
    return features;
  }

  public void setFeatures(Features features) {
    this.features = features;
  }

  public List<Navigation> getNavigation() {
    return navigation;
  }

  public void setNavigation(List<Navigation> navigation) {
    this.navigation = navigation;
  }

  public List<Option> getFeaturedServices() {
    return featuredServices;
  }

  public void setFeaturedServices(List<Option> featuredServices) {
    this.featuredServices = featuredServices;
  }

  public PlatformMessage getPlatformMessage() {
    return platformMessage;
  }

  public void setPlatformMessage(PlatformMessage platformMessage) {
    this.platformMessage = platformMessage;
  }

}
