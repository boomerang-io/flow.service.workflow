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
  private Map<String, Object> systemProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> globalProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> teamProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> workflowProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, String> taskInputProperties = new HashMap<>();

  public Map<String, String> getTaskInputProperties() {
    return taskInputProperties;
  }

  public void setTaskInputProperties(Map<String, String> taskInputProperties) {
    this.taskInputProperties = taskInputProperties;
  }

  public Map<String, Object> getGlobalProperties() {
    return globalProperties;
  }

  public void setGlobalProperties(Map<String, Object> globalProperties) {
    this.globalProperties = globalProperties;
  }

  public Map<String, Object> getSystemProperties() {
    return systemProperties;
  }

  public void setSystemProperties(Map<String, Object> systemProperties) {
    this.systemProperties = systemProperties;
  }

  public Map<String, Object> getTeamProperties() {
    return teamProperties;
  }

  public void setTeamProperties(Map<String, Object> teamProperties) {
    this.teamProperties = teamProperties;
  }
  
  public Map<String, Object> getWorkflowProperties() {
    return workflowProperties;
  }

  public void setWorkflowProperties(Map<String, Object> workflowProperties) {
    this.workflowProperties = workflowProperties;
  }

  @JsonAnyGetter
  public Map<String, String> getMap() {



    Map<String, String> finalProperties = new TreeMap<>();

    if (this.includeGlobalProperties) {
      copyProperties(globalProperties, finalProperties, "global");
    }

    copyProperties(teamProperties, finalProperties, "team");
    copyProperties(workflowProperties, finalProperties, "workflow");
    copyStringMap(taskInputProperties, finalProperties, "workflow");
    copyProperties(systemProperties, finalProperties, "system");

    return finalProperties;
  }

  public String getLayeredProperty(String key) {
    Object val = this.getMap().get(key);
    if (val instanceof Boolean) {
      return String.valueOf(val);
    } else {
      return this.getMap().get(key);
    }
  }

  private void copyProperties(Map<String, Object> source, Map<String, String> target,
      String prefix) {
    for (Entry<String, Object> entry : source.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value != null) {
        target.put(prefix + "/" + key, value.toString());
        target.put(key, value.toString());
      }
    }
  }

  private void copyStringMap(Map<String, String> source, Map<String, String> target,
      String prefix) {
    for (Entry<String, String> entry : source.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value != null) {
        target.put(prefix + "/" + key, value.toString());
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
}
