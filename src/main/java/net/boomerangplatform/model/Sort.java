package net.boomerangplatform.model;

import org.springframework.data.domain.Sort.Direction;

public class Sort {

  private String property;
  private Direction direction;
  
  public String getProperty() {
    return property;
  }
  public void setProperty(String property) {
    this.property = property;
  }
  public Direction getDirection() {
    return direction;
  }
  public void setDirection(Direction direction) {
    this.direction = direction;
  }
  
  
}
