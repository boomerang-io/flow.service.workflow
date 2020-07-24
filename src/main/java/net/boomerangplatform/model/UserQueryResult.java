package net.boomerangplatform.model;


import java.util.List;
import net.boomerangplatform.model.profile.PageableSummary;
import net.boomerangplatform.mongo.entity.FlowUserEntity;

public class UserQueryResult extends PageableSummary {

  private List<FlowUserEntity> records;

  public List<FlowUserEntity> getRecords() {
    return records;
  }

  public void setRecords(List<FlowUserEntity> records) {
    this.records = records;
  }

}
