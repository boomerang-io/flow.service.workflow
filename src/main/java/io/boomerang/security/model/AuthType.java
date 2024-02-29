package io.boomerang.security.model;

import java.util.HashMap;
import java.util.Map;

/*
 * Remains lowercase to match TokenTypePrefix and what a user would enter in json
 */
public enum AuthType {
  session("session"),user("user"),team("team"),workflow("workflow"),global("global");

  private String label;

  private static final Map<String, AuthType> BY_LABEL = new HashMap<>();
  
  AuthType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
  
  static {
      for (AuthType e: values()) {
        BY_LABEL.put(e.label, e);
      }
  }

  public static AuthType valueOfLabel(String label) {
    return BY_LABEL.get(label);
  }  
}
