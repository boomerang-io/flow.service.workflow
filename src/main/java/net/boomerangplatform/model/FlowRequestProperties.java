package net.boomerangplatform.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlowRequestProperties {

  private boolean includeGlobalProperties = true;

  @JsonIgnore
  private Map<String, Object> systemProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> globalProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> teamProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> stageProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> versionProperties = new HashMap<>();

  @JsonIgnore
  private Map<String, Object> componentProperties = new HashMap<>();

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

  public Map<String, Object> getStageProperties() {
    return stageProperties;
  }

  public void setStageProperties(Map<String, Object> stageProperties) {
    this.stageProperties = stageProperties;
  }

  public Map<String, Object> getVersionProperties() {
    return versionProperties;
  }

  public void setVersionProperties(Map<String, Object> versionProperties) {
    this.versionProperties = versionProperties;
  }

  public Map<String, Object> getComponentProperties() {
    return componentProperties;
  }

  public void setComponentProperties(Map<String, Object> componentProperties) {
    this.componentProperties = componentProperties;
  }

  @JsonAnyGetter
  public Map<String, Object> getMap() {

    Map<String, Object> finalProperties = new TreeMap<>();

    if (this.includeGlobalProperties) {
      copyProperties(globalProperties, finalProperties, "global");
    }

    copyProperties(teamProperties, finalProperties, "team");
    copyProperties(stageProperties, finalProperties, "stage");
    copyProperties(componentProperties, finalProperties, "component");
    copyProperties(versionProperties, finalProperties, "version");
    copyProperties(systemProperties, finalProperties, "system");
    return finalProperties;
  }

  public String getLayeredProperty(String key) {
    Object val = this.getMap().get(key);
    if (val instanceof Boolean) {
      return String.valueOf(val);
    } else {
      return (String) this.getMap().get(key);
    }
  }

  private void copyProperties(Map<String, Object> source, Map<String, Object> target,
      String prefix) {
    for (Entry<String, Object> entry : source.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      target.put(prefix + "/" + key, value);
      target.put(key, value);
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

