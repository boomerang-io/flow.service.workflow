package io.boomerang.model;

import java.util.List;
import io.boomerang.model.profile.PageableSummary;
import io.boomerang.v4.model.Team;

public class TeamQueryResult extends PageableSummary {

  private List<Team> records;

  public List<Team> getRecords() {
    return records;
  }

  public void setRecords(List<Team> records) {
    this.records = records;
  }
}
