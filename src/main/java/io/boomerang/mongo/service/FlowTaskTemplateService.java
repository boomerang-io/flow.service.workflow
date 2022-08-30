package io.boomerang.mongo.service;

import java.util.List;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;

public interface FlowTaskTemplateService {

  FlowTaskTemplateEntity getTaskTemplateWithId(String id);

  List<FlowTaskTemplateEntity> getAllTaskTemplates();

  FlowTaskTemplateEntity insertTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);

  FlowTaskTemplateEntity updateTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);

  void deleteTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);

  void activateTaskTemplate(FlowTaskTemplateEntity flowTaskTemplateEntity);
  
  List<FlowTaskTemplateEntity> getAllTaskTemplatesforTeamId(String teamId);
  
  List<FlowTaskTemplateEntity> getAllTaskTemplatesForSystem();
  
  List<FlowTaskTemplateEntity> getTaskTemplatesforTeamId(String teamId);
 
  List<FlowTaskTemplateEntity> getAllSystemTasks();
  
  List<FlowTaskTemplateEntity> getAllGlobalTasks();
  
  List<FlowTaskTemplateEntity> getTaskTemplateWithIds(List<String> ids);
  
}
