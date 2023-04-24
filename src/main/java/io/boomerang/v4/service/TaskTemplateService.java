package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.client.TaskTemplateResponsePage;
import io.boomerang.v4.data.entity.ref.TaskTemplateEntity;
import io.boomerang.v4.model.ref.TaskTemplate;

public interface TaskTemplateService {
//
//  TektonTask getTaskTemplateYamlWithId(String id);
//
//  TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber);
//
//  FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask,TemplateScope scope, String teamId);
//
//  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask);
//
//  FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask, Integer revision, String comment);
//
//  List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId);
//
//  FlowTaskTemplate validateTaskTemplate(TektonTask tektonTask);

  ResponseEntity<TaskTemplate> get(String name, Optional<Integer> version);

  TaskTemplateResponsePage query(int page, int limit, Sort sort, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> names, Optional<List<String>> teams);
}
