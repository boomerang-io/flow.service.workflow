package net.boomerangplatform.service;

import net.boomerangplatform.model.Task;
import net.boomerangplatform.service.refactor.ControllerRequestProperties;

public interface PropertyManager {

  ControllerRequestProperties buildRequestPropertyLayering(Task task, String activityId);

  public String replaceValueWithProperty(String value, String activityId,
      ControllerRequestProperties properties);

}
