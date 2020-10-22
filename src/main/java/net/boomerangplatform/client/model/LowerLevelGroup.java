package net.boomerangplatform.client.model;

import org.springframework.data.mongodb.core.mapping.Field;

public class LowerLevelGroup {

  @Field("id")
  private String id;
  private Boolean visible;


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Boolean getVisible() {
    return visible;
  }

  public void setVisible(Boolean visible) {
    this.visible = visible;
  }


}
