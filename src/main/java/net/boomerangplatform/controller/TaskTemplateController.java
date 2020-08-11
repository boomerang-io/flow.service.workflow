package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.FlowTaskTemplate;
import net.boomerangplatform.mongo.model.FlowTaskTemplateStatus;
import net.boomerangplatform.service.crud.TaskTemplateService;

@RestController
@RequestMapping("/workflow/tasktemplate")
public class TaskTemplateController {

  @Autowired
  private TaskTemplateService taskTemplateService;

  @GetMapping(value = "{id}")
  public FlowTaskTemplate getTaskTemplateWithId(@PathVariable String id) {
    return taskTemplateService.getTaskTemplateWithId(id);
  }

  @GetMapping(value = "")
  public List<FlowTaskTemplate> getAllTaskTemplates() {
    return taskTemplateService.getAllTaskTemplates();
  }

  @PostMapping(value = "")
  public FlowTaskTemplate insertTaskTemplate(@RequestBody FlowTaskTemplate flowTaskTemplateEntity) {
    flowTaskTemplateEntity.setStatus(FlowTaskTemplateStatus.active);

    return taskTemplateService.insertTaskTemplate(flowTaskTemplateEntity);
  }

  @PutMapping(value = "")
  public FlowTaskTemplate updateTaskTemplate(@RequestBody FlowTaskTemplate flowTaskTemplateEntity) {
    flowTaskTemplateEntity.setStatus(FlowTaskTemplateStatus.active);
    return taskTemplateService.updateTaskTemplate(flowTaskTemplateEntity);
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
