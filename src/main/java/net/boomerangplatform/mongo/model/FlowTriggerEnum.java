package net.boomerangplatform.mongo.model;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FlowTriggerEnum {
  manual("manual"), scheduler("scheduler"), webhook("webhook"),slack("slack"), dockerhub("dockerhub"), custom("custom");

  private String trigger;

  FlowTriggerEnum(String trigger) {
    this.trigger = trigger;
  }

  @JsonValue
  public String getTrigger() {
    return trigger;
  }

  public static FlowTriggerEnum getFlowTriggerEnum(String flowTriggerEnum) {
    return Arrays.asList(FlowTriggerEnum.values()).stream()
        .filter(value -> value.getTrigger().equals(flowTriggerEnum)).findFirst().orElse(null);
  }
}
