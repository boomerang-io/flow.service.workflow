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

  private boolean includeGlobalParams = true;

  @JsonIgnore
  private Map<String, Object> systemParams = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> globalParams = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> teamParams = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> workflowParams = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> taskParams = new HashMap<>();

  public Map<String, Object> getTaskInputProperties() {
    return taskParams;
  }

  public void setTaskInputProperties(Map<String, Object> taskInputProperties) {
    this.taskParams = taskInputProperties;
  }

  public Map<String, Object> getGlobalProperties() {
    return globalParams;
  }

  public void setGlobalProperties(Map<String, Object> globalProperties) {
    this.globalParams = globalProperties;
  }

  public Map<String, Object> getSystemProperties() {
    return systemParams;
  }

  public void setSystemProperties(Map<String, Object> systemProperties) {
    this.systemParams = systemProperties;
  }

  public Map<String, Object> getTeamProperties() {
    return teamParams;
  }

  public void setTeamProperties(Map<String, Object> teamProperties) {
    this.teamParams = teamProperties;
  }

  public Map<String, Object> getWorkflowProperties() {
    return workflowParams;
  }

  public void setWorkflowProperties(Map<String, Object> workflowProperties) {
    this.workflowParams = workflowProperties;
  }

  @JsonAnyGetter
  public Map<String, Object> getMap(boolean includeScope) {

    Map<String, Object> finalProperties = new TreeMap<>();

    if (this.includeGlobalParams) {
      copyProperties(globalParams, finalProperties, "global", includeScope);
    }

    copyProperties(teamParams, finalProperties, "team", includeScope);
    copyProperties(workflowParams, finalProperties, "workflow", includeScope);
    copyProperties(taskParams, finalProperties, null, includeScope);
    copyProperties(systemParams, finalProperties, "system", includeScope);

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

    if (this.includeGlobalParams) {
      copyFlatProperties(globalParams, finalProperties, "global");
    }

    copyFlatProperties(teamParams, finalProperties, "team");
    copyFlatProperties(workflowParams, finalProperties, "workflow");
    copyFlatProperties(taskParams, finalProperties, null);
    copyFlatProperties(systemParams, finalProperties, "system");

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
    return includeGlobalParams;
  }


  public void setIncludeGlobalProperties(boolean includeGlobalProperties) {
    this.includeGlobalParams = includeGlobalProperties;
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
