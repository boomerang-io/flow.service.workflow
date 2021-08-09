package io.boomerang.model;

import java.util.List;
import io.boomerang.model.profile.PageableSummary;

public class TeamQueryResult extends PageableSummary {

  private List<FlowTeam> records;

  public List<FlowTeam> getRecords() {
    return records;
  }

  public void setRecords(List<FlowTeam> records) {
    this.records = records;
  }
}
