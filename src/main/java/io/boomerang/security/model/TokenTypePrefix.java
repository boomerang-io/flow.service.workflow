package io.boomerang.security.model;

import java.util.HashMap;
import java.util.Map;

public enum TokenTypePrefix {
  global("bfg"), team("bft"), workflow("bff"), user("bfu");

  public final String label;

  private static final Map<String, TokenTypePrefix> BY_LABEL = new HashMap<>();

  private TokenTypePrefix(String label) {
    this.label = label;
  }
  
  static {
      for (TokenTypePrefix e: values()) {
          BY_LABEL.put(e.label, e);
      }
  }

  public static TokenTypePrefix valueOfLabel(String label) {
    return BY_LABEL.get(label);
  }
}
