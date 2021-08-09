package io.boomerang.service.crud;

import java.util.List;
import io.boomerang.model.FlowTaskTemplate;
import io.boomerang.model.TemplateScope;
import io.boomerang.model.tekton.TektonTask;

public interface TaskTemplateService {
  FlowTaskTemplate getTaskTemplateWithId(String id);

  TektonTask getTaskTemplateYamlWithId(String id);
  
  List<FlowTaskTemplate> getAllTaskTemplates(TemplateScope scope, String teamId);

  FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity);

  void deleteTaskTemplateWithId(String id);

  void activateTaskTemplate(String id);

  TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber);

  FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask,TemplateScope scope, String teamId);

  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask);

  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask, Integer revision, String comment);

  List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId);

  FlowTaskTemplate validateTaskTemplate(TektonTask tektonTask);
}
