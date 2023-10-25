package io.boomerang.model.ref;

import java.util.List;
import io.boomerang.model.enums.TriggerEnum;

public class WorkflowTrigger {

  private Boolean enabled = Boolean.FALSE;
  private TriggerEnum type;
  private List<WorkflowTriggerCondition> conditions;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public TriggerEnum getType() {
    return type;
  }

  public void setType(TriggerEnum type) {
    this.type = type;
  }

  public List<WorkflowTriggerCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<WorkflowTriggerCondition> conditions) {
    this.conditions = conditions;
  }
}
