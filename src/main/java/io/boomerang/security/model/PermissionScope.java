package io.boomerang.security.model;

import java.util.HashMap;
import java.util.Map;

public enum PermissionScope {
  SYSTEM("system"), WORKFLOW("workflow"), WORKFLOWRUN("workflowrun"), WORKFLOWTEMPLATE("workflowtemplate"), TASKRUN("taskrun"), TASK("task"), 
  ACTION("action"), USER("user"), TEAM("team"), TOKEN("token"), PARAMETER("parameter"), SCHEDULE("schedule"), INSIGHTS("insights"), INTEGRATION("integration"), ANY("**");

  private String label;

  private static final Map<String, PermissionScope> BY_LABEL = new HashMap<>();
  
  PermissionScope(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
  
  static {
      for (PermissionScope e: values()) {
        BY_LABEL.put(e.label, e);
      }
  }

  public static PermissionScope valueOfLabel(String label) {
    return BY_LABEL.get(label);
  }
  
}
