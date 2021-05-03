package net.boomerangplatform.service.refactor;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ControllerRequestProperties {

  private boolean includeGlobalProperties = true;

  @JsonIgnore
  private Map<String, String> systemProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, String> globalProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, String> teamProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, String> workflowProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, String> taskInputProperties = new HashMap<>();
  
  @JsonIgnore
  private Map<String, String> reservedProperties = new HashMap<>();


  public Map<String, String> getReservedProperties() {
    return reservedProperties;
  }

  public void setReservedProperties(Map<String, String> reservedProperties) {
    this.reservedProperties = reservedProperties;
  }

  public Map<String, String> getTaskInputProperties() {
    return taskInputProperties;
  }

  public void setTaskInputProperties(Map<String, String> taskInputProperties) {
    this.taskInputProperties = taskInputProperties;
  }

  public Map<String, String> getGlobalProperties() {
    return globalProperties;
  }

  public void setGlobalProperties(Map<String, String> globalProperties) {
    this.globalProperties = globalProperties;
  }

  public Map<String, String> getSystemProperties() {
    return systemProperties;
  }

  public void setSystemProperties(Map<String, String> systemProperties) {
    this.systemProperties = systemProperties;
  }

  public Map<String, String> getTeamProperties() {
    return teamProperties;
  }

  public void setTeamProperties(Map<String, String> teamProperties) {
    this.teamProperties = teamProperties;
  }

  public Map<String, String> getWorkflowProperties() {
    return workflowProperties;
  }

  public void setWorkflowProperties(Map<String, String> workflowProperties) {
    this.workflowProperties = workflowProperties;
  }

  @JsonAnyGetter
  public Map<String, String> getMap(boolean includeScope) {

    Map<String, String> finalProperties = new TreeMap<>();

    if (this.includeGlobalProperties) {
      copyProperties(globalProperties, finalProperties, "global", includeScope);
    }

    copyProperties(teamProperties, finalProperties, "team", includeScope);
    copyProperties(workflowProperties, finalProperties, "workflow", includeScope);
    copyStringMap(taskInputProperties, finalProperties, "workflow", includeScope);
    copyProperties(systemProperties, finalProperties, "system", includeScope);

    copyProperties( this.getReservedProperties(), finalProperties, null, false);


    return finalProperties;
  }

  public String getLayeredProperty(String key) {
    Object val = this.getMap(false).get(key);
    if (val instanceof Boolean) {
      return String.valueOf(val);
    } else {
      return this.getMap(false).get(key);
    }
  }

  private void copyProperties(Map<String, String> source, Map<String, String> target, String prefix,
      boolean inludeScope) {
    for (Entry<String, String> entry : source.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value != null) {
        if (inludeScope) {
          target.put(prefix + "/" + key, value.toString());
        }
        target.put(key, value.toString());
      }
    }
  }

  private void copyStringMap(Map<String, String> source, Map<String, String> target, String prefix,
      boolean includeScope) {
    for (Entry<String, String> entry : source.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value != null) {
        if (includeScope) {
          target.put(prefix + "/" + key, value.toString());
        }
        target.put(key, value.toString());
      }
    }
  }


  @Override
  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    String jsonResult;
    try {
      jsonResult = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return "";
    }
    return jsonResult;
  }


  public boolean isIncludeGlobalProperties() {
    return includeGlobalProperties;
  }


  public void setIncludeGlobalProperties(boolean includeGlobalProperties) {
    this.includeGlobalProperties = includeGlobalProperties;
  }

  public Map<String, String> getMapForKey(String key) {
    if ("workflow".equals(key)) {
      return this.getWorkflowProperties();
    } else if ("system".equals(key)) {
      return this.getSystemProperties();
    } else if ("team".equals(key)) {
      return this.getTeamProperties();
    } else if ("global".equals(key)) {
      return this.getGlobalProperties();
    } else {
      return new HashMap<>();
    }
  }
}
