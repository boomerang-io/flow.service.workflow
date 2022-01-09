package io.boomerang.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.util.ParameterMapper;

public class WorkflowSchedule extends WorkflowScheduleEntity {
  
  /** 
   * We extend the entity and utilize a map of parameters for external consumption
   * This will then be run through the ParameterMapper util function to save on the
   * entity as a parameters KeyValuePair list.
   */
  @JsonProperty("parameters")
  private Map<String, String> parametersMap = new HashMap<>();
  
  private Date nextScheduleDate;

  public WorkflowSchedule() {
  }

  public WorkflowSchedule(WorkflowScheduleEntity entity) {
    BeanUtils.copyProperties(entity, this, "parameters");
    this.parametersMap = ParameterMapper.keyValuePairListToMap(entity.getParameters());
  }

  public Map<String, String> getParametersMap() {
    return parametersMap;
  }

  public void setParametersMap(Map<String, String> parametersMap) {
    this.parametersMap = parametersMap;
  }

  public Date getNextScheduleDate() {
    return nextScheduleDate;
  }

  public void setNextScheduleDate(Date nextScheduleDate) {
    this.nextScheduleDate = nextScheduleDate;
  }
}
