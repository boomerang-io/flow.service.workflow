package io.boomerang.model;


import java.util.List;
import io.boomerang.model.profile.PageableSummary;
import io.boomerang.v4.data.entity.UserEntity;

public class UserQueryResult extends PageableSummary {

  private List<UserEntity> records;

  public List<UserEntity> getRecords() {
    return records;
  }

  public void setRecords(List<UserEntity> records) {
    this.records = records;
  }

}
