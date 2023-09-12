package io.boomerang.model.enums.ref;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskType {
  start("start"), end("end"), template("template"), custom("custom"), generic("generic"), decision("decision"), // NOSONAR
  approval("approval"), setwfproperty("setwfproperty"), manual("manual)"), eventwait("eventwait"), acquirelock("acquirelock"), // NOSONAR 
  releaselock("releaselock"), runworkflow("runworkflow"), runscheduledworkflow("runscheduledworkflow"), script("script"), // NOSONAR 
  setwfstatus("setwfstatus"), sleep("sleep"); // NOSONAR
  
  private String type;

  TaskType(String type) {
    this.type = type;
  }

  @JsonValue
  public String getType() {
    return type;
  }

  public static TaskType getRunType(String type) {
    return Arrays.asList(TaskType.values()).stream()
        .filter(value -> value.getType().equals(type)).findFirst().orElse(null);
  }
}
