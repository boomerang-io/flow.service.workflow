package io.boomerang.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.FlowTaskTemplate;
import io.boomerang.model.TemplateScope;
import io.boomerang.model.tekton.TektonTask;
import io.boomerang.mongo.model.FlowTaskTemplateStatus;
import io.boomerang.service.crud.TaskTemplateService;

@RestController
@RequestMapping("/workflow/tasktemplate")
public class TaskTemplateController {

  @Autowired
  private TaskTemplateService taskTemplateService;

  @PostMapping(value = "yaml/validate", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate validateYaml(@RequestBody TektonTask tektonTask) {
    return taskTemplateService.validateTaskTemplate(tektonTask);
  }
  
  @GetMapping(value = "{id}")
  public FlowTaskTemplate getTaskTemplateWithId(@PathVariable String id) {
    return taskTemplateService.getTaskTemplateWithId(id);
  }

  @GetMapping(value = "{id}/yaml",  produces = "application/x-yaml")
  public TektonTask getTaskTemplateYamlWithId(@PathVariable String id) {
    return taskTemplateService.getTaskTemplateYamlWithId(id);
  }
  
  @GetMapping(value = "{id}/yaml/{revision}",  produces = "application/x-yaml")
  public TektonTask getTaskTemplateYamlWithIdandRevision(@PathVariable String id, @PathVariable Integer revision ) {
    return taskTemplateService.getTaskTemplateYamlWithIdAndRevision(id, revision);
  }
  
  
  @GetMapping(value = "")
  public List<FlowTaskTemplate> getAllTaskTemplates(@RequestParam(required = false) TemplateScope scope, @RequestParam(required = false) String teamId) {
    return taskTemplateService.getAllTaskTemplates(scope, teamId);
  }

  @GetMapping(value = "/workflow/{workflowId}")
  public List<FlowTaskTemplate> getTaskTemplatesForWorkfow(@PathVariable String workflowId) {
    return taskTemplateService.getAllTaskTemplatesForWorkfow(workflowId);
  }
  
  @PostMapping(value = "")
  public FlowTaskTemplate insertTaskTemplate(@RequestBody FlowTaskTemplate flowTaskTemplateEntity) {
    flowTaskTemplateEntity.setStatus(FlowTaskTemplateStatus.active);
    return taskTemplateService.insertTaskTemplate(flowTaskTemplateEntity);
  }
  
  @PostMapping(value = "yaml", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate insertTaskTemplateYaml(@RequestBody TektonTask tektonTask, @RequestParam(required = true) TemplateScope scope, @RequestParam(required = false) String teamId) {
    return taskTemplateService.insertTaskTemplateYaml(tektonTask, scope, teamId);
  }

  @PutMapping(value = "")
  public FlowTaskTemplate updateTaskTemplate(@RequestBody FlowTaskTemplate flowTaskTemplateEntity) {
    flowTaskTemplateEntity.setStatus(FlowTaskTemplateStatus.active);
    return taskTemplateService.updateTaskTemplate(flowTaskTemplateEntity);
  }
  
  @PutMapping(value = "{id}/yaml", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate updateTaskTemplateWithYaml(@PathVariable String id, @RequestBody TektonTask tektonTask) {
    return taskTemplateService.updateTaskTemplateWithYaml(id, tektonTask);
  }
  
  @PutMapping(value = "{id}/yaml/{revision}", consumes = "application/x-yaml", produces = "application/json")
  public FlowTaskTemplate updateTaskTemplateWithYamlForRevision(@PathVariable String id, @RequestBody TektonTask tektonTask, @PathVariable Integer revision,  @RequestParam(required = false) String comment) {
    return taskTemplateService.updateTaskTemplateWithYaml(id, tektonTask, revision, comment);
  }
  
  @DeleteMapping(value = "{id}")
  public void deleteTaskTemplateWithId(@PathVariable String id) {
    taskTemplateService.deleteTaskTemplateWithId(id);
  }

  @PutMapping("/{id}/activate")
  public void activateTaskTemplate(@PathVariable String id) {
    taskTemplateService.activateTaskTemplate(id);
  }
}
