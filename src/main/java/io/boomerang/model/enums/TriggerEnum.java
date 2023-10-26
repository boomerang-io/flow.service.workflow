package io.boomerang.model.enums;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TriggerEnum {
  manual("manual"), schedule("schedule"), webhook("webhook"), event("event"), github("github");

  private String trigger;

  TriggerEnum(String trigger) {
    this.trigger = trigger;
  }

  @JsonValue
  public String getTrigger() {
    return trigger;
  }

  public static TriggerEnum getFlowTriggerEnum(String flowTriggerEnum) {
    return Arrays.asList(TriggerEnum.values()).stream()
        .filter(value -> value.getTrigger().equals(flowTriggerEnum)).findFirst().orElse(null);
  }
}
