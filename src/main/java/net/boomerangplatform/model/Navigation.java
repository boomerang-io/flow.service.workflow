package net.boomerangplatform.model;

import java.util.List;

public class Navigation {

  private String name;
  private String icon;
  private String type;
  private String link;
  private List<Navigation> childLinks;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public List<Navigation> getChildLinks() {
    return childLinks;
  }

  public void setChildLinks(List<Navigation> childLinks) {
    this.childLinks = childLinks;
  }



}
