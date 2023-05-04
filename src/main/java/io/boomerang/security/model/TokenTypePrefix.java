package io.boomerang.security.model;

import java.util.HashMap;
import java.util.Map;

public enum TokenTypePrefix {
  global("bfg"), team("bft"), workflow("bff"), user("bfu"), session("bfs");

  public final String prefix;

  private static final Map<String, TokenTypePrefix> BY_PREFIX = new HashMap<>();

  private TokenTypePrefix(String prefix) {
    this.prefix = prefix;
  }
  
  public String getPrefix() {
    return prefix;
  }
  
  static {
      for (TokenTypePrefix e: values()) {
        BY_PREFIX.put(e.prefix, e);
      }
  }

  public static TokenTypePrefix valueOfPrefix(String prefix) {
    return BY_PREFIX.get(prefix);
  }
  
//  public final String prefix;
//
//  TokenTypePrefix(String prefix) {
//    this.prefix = prefix;
//  }
//
//  public String valueOf() {
//    return prefix;
//  }
}
