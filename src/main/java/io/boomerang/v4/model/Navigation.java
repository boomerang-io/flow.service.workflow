package io.boomerang.v4.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "type", "icon", "link", "childLinks"})
public class Navigation {

  private String name;
  private String icon;
  private NavigationType type;
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

  public NavigationType getType() {
    return type;
  }

  public void setType(NavigationType type) {
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
