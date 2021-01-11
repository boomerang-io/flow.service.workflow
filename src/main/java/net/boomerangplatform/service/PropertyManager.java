package net.boomerangplatform.service;

import java.util.Map;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.service.refactor.ControllerRequestProperties;

public interface PropertyManager {

  ControllerRequestProperties buildRequestPropertyLayering(Task task, String activityId, String workflowId);

  public String replaceValueWithProperty(String value, String activityId,
      ControllerRequestProperties properties);

  public void buildSystemProperties(Task task, String activityId, String workflowId,
      Map<String, Object> systemProperties);

  public void buildWorkflowProperties(Map<String, Object> workflowProperties, String activityId,
      String workflowId);

  public void buildGlobalProperties(Map<String, Object> globalProperties);

  public void buildTeamProperties(Map<String, Object> teamProperties, String workflowId);

}
