package net.boomerangplatform.model.profile;

public class SortSummary {

  private String direction = "DESC";
  private String property = "createdDate";
  private boolean ignoreCase;
  private String nullHandling = "NATIVE";
  private boolean descending;
  private boolean ascending;

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public boolean isIgnoreCase() {
    return ignoreCase;
  }

  public void setIgnoreCase(boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
  }

  public String getNullHandling() {
    return nullHandling;
  }

  public void setNullHandling(String nullHandling) {
    this.nullHandling = nullHandling;
  }

  public boolean isDescending() {
    return descending;
  }

  public void setDescending(boolean descending) {
    this.descending = descending;
  }

  public boolean isAscending() {
    return ascending;
  }

  public void setAscending(boolean ascending) {
    this.ascending = ascending;
  }

}
