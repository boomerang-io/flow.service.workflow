package io.boomerang.model.ref;

import java.util.LinkedList;
import java.util.List;

// TODO: implement a more generic trigger with list of triggers and a type rather than fixed.
public class Trigger {

  private Boolean enabled = Boolean.FALSE;
  // private TriggerEnum type;
  private List<TriggerCondition> conditions = new LinkedList<>();

  public Trigger() {}

  public Trigger(Boolean enabled) {
    this.enabled = enabled;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  // public TriggerEnum getType() {
  // return type;
  // }
  //
  // public void setType(TriggerEnum type) {
  // this.type = type;
  // }

  public List<TriggerCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<TriggerCondition> conditions) {
    this.conditions = conditions;
  }
}
