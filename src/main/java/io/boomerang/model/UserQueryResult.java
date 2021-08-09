package io.boomerang.model;


import java.util.List;
import io.boomerang.model.profile.PageableSummary;
import io.boomerang.mongo.entity.FlowUserEntity;

public class UserQueryResult extends PageableSummary {

  private List<FlowUserEntity> records;

  public List<FlowUserEntity> getRecords() {
    return records;
  }

  public void setRecords(List<FlowUserEntity> records) {
    this.records = records;
  }

}
