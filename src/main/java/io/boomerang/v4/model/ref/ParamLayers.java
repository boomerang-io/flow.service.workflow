package io.boomerang.v4.model.ref;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.TreeMap;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParamLayers {
  private static final Logger LOGGER = LogManager.getLogger();

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
  private Map<String, Object> taskInputProperties = new HashMap<>();
  
  @JsonIgnore
  private Map<String, Object> reservedProperties = new HashMap<>();


  public Map<String, Object> getReservedProperties() {
    return reservedProperties;
  }

  public void setReservedProperties(Map<String, Object> reservedProperties) {
    this.reservedProperties = reservedProperties;
  }

  public Map<String, Object> getTaskInputProperties() {
    return taskInputProperties;
  }

  public void setTaskInputProperties(Map<String, Object> taskInputProperties) {
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
  public Map<String, Object> getMap(boolean includeScope) {

    Map<String, Object> finalProperties = new TreeMap<>();

    if (this.includeGlobalProperties) {
      copyProperties(globalProperties, finalProperties, "global", includeScope);
    }

    copyProperties(teamProperties, finalProperties, "team", includeScope);
    copyProperties(workflowProperties, finalProperties, "workflow", includeScope);
    copyProperties(taskInputProperties, finalProperties, null, includeScope);
    copyProperties(systemProperties, finalProperties, "system", includeScope);

    copyProperties( this.getReservedProperties(), finalProperties, null, false);


    return finalProperties;
  }

  public String getLayeredProperty(String key) {
    Object val = this.getMap(false).get(key);
    if (val instanceof Boolean) {
      return String.valueOf(val);
    } else {
      return this.getMap(false).get(key).toString();
    }
  }

  private void copyProperties(Map<String, Object> source, Map<String, Object> target, String prefix,
      boolean inludeScope) {
    for (Entry<String, Object> entry : source.entrySet()) {
      LOGGER.debug("Parameter: " + entry.toString());
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

  @JsonAnyGetter
  public Map<String, Object> getFlatMap() {

    Map<String, Object> finalProperties = new TreeMap<>();

    if (this.includeGlobalProperties) {
      copyFlatProperties(globalProperties, finalProperties, "global");
    }

    copyFlatProperties(teamProperties, finalProperties, "team");
    copyFlatProperties(workflowProperties, finalProperties, "workflow");
    copyFlatProperties(taskInputProperties, finalProperties, null);
    copyFlatProperties(systemProperties, finalProperties, "system");

    copyFlatProperties( this.getReservedProperties(), finalProperties, null);


    return finalProperties;
  }

  private void copyFlatProperties(Map<String, Object> source, Map<String, Object> target, String prefix) {
    for (Entry<String, Object> entry : source.entrySet()) {
      LOGGER.debug("Parameter: " + entry.toString());
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value != null) {
        if (prefix != null) {
          target.put(prefix + ".params." + key, value);
        }
        target.put("params." + key, value);
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

  public Map<String, Object> getMapForKey(String key) {
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
