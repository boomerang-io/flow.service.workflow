package net.boomerangplatform.service.crud;

import java.util.List;
import net.boomerangplatform.model.FlowTaskTemplate;

public interface TaskTemplateService {
  FlowTaskTemplate getTaskTemplateWithId(String id);

  List<FlowTaskTemplate> getAllTaskTemplates();

  FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  void deleteTaskTemplateWithId(String id);

  void activateTaskTemplate(String id);
}
