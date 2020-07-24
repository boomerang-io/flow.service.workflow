package net.boomerangplatform.mongo.service;

import java.util.List;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;

public interface FlowTaskTemplateService {

  FlowTaskTemplateEntity getTaskTemplateWithId(String id);

  List<FlowTaskTemplateEntity> getAllTaskTemplates();

  FlowTaskTemplateEntity insertTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);

  FlowTaskTemplateEntity updateTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);

  void deleteTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);

  void activateTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);
}
