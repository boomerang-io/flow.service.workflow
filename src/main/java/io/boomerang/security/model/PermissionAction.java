package io.boomerang.security.model;

import java.util.HashMap;
import java.util.Map;

public enum PermissionAction {
  READ("Read"), WRITE("Write"), DELETE("Delete"), ACTION("Action");

  private String label;

  private static final Map<String, PermissionAction> BY_LABEL = new HashMap<>();
  
  PermissionAction(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
  
  static {
      for (PermissionAction e: values()) {
        BY_LABEL.put(e.label, e);
      }
  }

  public static PermissionAction valueOfLabel(String label) {
    return BY_LABEL.get(label);
  }
  
}
