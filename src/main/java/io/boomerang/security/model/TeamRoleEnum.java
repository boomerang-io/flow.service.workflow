package io.boomerang.security.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TeamRoleEnum {
  OWNER("owner"), EDITOR("editor"), READER("reader");

  private String label;

  private static final Map<String, TeamRoleEnum> BY_LABEL = new HashMap<>();

  TeamRoleEnum(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
  
  static {
      for (TeamRoleEnum e: values()) {
        BY_LABEL.put(e.label, e);
      }
  }

  public static TeamRoleEnum valueOfLabel(String label) {
    return BY_LABEL.get(label);
  }
  
}
