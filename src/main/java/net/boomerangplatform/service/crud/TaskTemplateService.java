package net.boomerangplatform.service.crud;

import java.util.List;
import net.boomerangplatform.model.FlowTaskTemplate;
import net.boomerangplatform.model.TemplateScope;
import net.boomerangplatform.model.tekton.TektonTask;

public interface TaskTemplateService {
  FlowTaskTemplate getTaskTemplateWithId(String id);

  TektonTask getTaskTemplateYamlWithId(String id);
  
  List<FlowTaskTemplate> getAllTaskTemplates(TemplateScope scope, String teamId);

  FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  void deleteTaskTemplateWithId(String id);

  void activateTaskTemplate(String id);

  TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber);

  FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask);

  FlowTaskTemplate updateTaskTemplateWuthYaml(String id, TektonTask tektonTask);

  FlowTaskTemplate updateTaskTemplateWuthYaml(String id, TektonTask tektonTask, Integer revision);

  List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId);
}
