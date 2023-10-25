package io.boomerang.model.ref;

import java.util.List;
import io.boomerang.model.enums.TriggerConditionOperation;

public class WorkflowTriggerCondition {
  
  private String field;
  private TriggerConditionOperation operation;
  private String value;
  private List<String> values;
  public String getField() {
    return field;
  }
  public void setField(String field) {
    this.field = field;
  }
  public TriggerConditionOperation getOperation() {
    return operation;
  }
  public void setOperation(TriggerConditionOperation operation) {
    this.operation = operation;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }
  public List<String> getValues() {
    return values;
  }
  public void setValues(List<String> values) {
    this.values = values;
  }
}
