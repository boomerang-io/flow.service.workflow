package net.boomerangplatform.service.crud;

import java.util.List;
import net.boomerangplatform.model.FlowTaskTemplate;
import net.boomerangplatform.model.tekton.TektonTask;

public interface TaskTemplateService {
  FlowTaskTemplate getTaskTemplateWithId(String id);

  TektonTask getTaskTemplateYamlWithId(String id);
  
  List<FlowTaskTemplate> getAllTaskTemplates();

  FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  void deleteTaskTemplateWithId(String id);

  void activateTaskTemplate(String id);
}
