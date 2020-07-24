package net.boomerangplatform.model;

import java.util.List;
import net.boomerangplatform.model.profile.PageableSummary;

public class TeamQueryResult extends PageableSummary {

  private List<FlowTeam> records;

  public List<FlowTeam> getRecords() {
    return records;
  }

  public void setRecords(List<FlowTeam> records) {
    this.records = records;
  }
}
