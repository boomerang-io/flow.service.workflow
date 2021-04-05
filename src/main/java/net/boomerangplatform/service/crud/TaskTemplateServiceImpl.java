package net.boomerangplatform.service.crud;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.FlowTaskTemplate;
import net.boomerangplatform.model.TemplateScope;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.model.controller.Workflow;
import net.boomerangplatform.model.tekton.TektonTask;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.ChangeLog;
import net.boomerangplatform.mongo.model.FlowTaskTemplateStatus;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.model.WorkflowScope;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.service.UserIdentityService;
import net.boomerangplatform.service.tekton.TetkonConverter;

@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {

  @Autowired
  private FlowTaskTemplateService flowTaskTemplateService;

  @Autowired
  private UserIdentityService userIdentityService;
  
  @Autowired
  private WorkflowService workflowService;

  @Override
  public FlowTaskTemplate getTaskTemplateWithId(String id) {
    FlowTaskTemplateEntity entity = flowTaskTemplateService.getTaskTemplateWithId(id);
    if (entity != null) {
      FlowTaskTemplate template = new FlowTaskTemplate(entity);
      for (Revision revision : template.getRevisions()) {
        if (revision.getChangelog() != null && revision.getChangelog().getUserId() != null) {
          FlowUserEntity user =
              userIdentityService.getUserByID(revision.getChangelog().getUserId());
          if (revision.getChangelog() != null && user != null
              && revision.getChangelog().getUserName() == null) {
            revision.getChangelog().setUserName(user.getName());
          }
        }
      }
      return template;
    }
    return null;
  }

  @Override
  public List<FlowTaskTemplate> getAllTaskTemplates(TemplateScope scope, String teamId) {
    List<FlowTaskTemplate> templates = new LinkedList<>();
    
    if (scope == TemplateScope.global || scope == null) {
       templates = flowTaskTemplateService.getAllGlobalTasks().stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    } else if (scope == TemplateScope.team) {
       templates = flowTaskTemplateService.getTaskTemplatesforTeamId(teamId).stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    } else if (scope == TemplateScope.system) {
       templates = flowTaskTemplateService.getAllSystemTasks().stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    }
    
    updateTemplateListUserNames(templates);
    return templates;
  }

  private void updateTemplateListUserNames(List<FlowTaskTemplate> templates) {
    for (FlowTaskTemplate template : templates) {
      for (Revision revision : template.getRevisions()) {
        if (revision.getChangelog() != null && revision.getChangelog().getUserId() != null) {
          FlowUserEntity user =
              userIdentityService.getUserByID(revision.getChangelog().getUserId());
          if (revision.getChangelog() != null && user != null
              && revision.getChangelog().getUserName() == null) {
            revision.getChangelog().setUserName(user.getName());
          }
        }
      }
    }
  }

  @Override
  public FlowTaskTemplate insertTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity) {
    Date creationDate = new Date();

    flowTaskTemplateEntity.setCreatedDate(creationDate);
    flowTaskTemplateEntity.setLastModified(creationDate);
    flowTaskTemplateEntity.setVerified(false);

    updateChangeLog(flowTaskTemplateEntity);

    return new FlowTaskTemplate(flowTaskTemplateService.insertTaskTemplate(flowTaskTemplateEntity));
  }

  @Override
  public FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity) {
    updateChangeLog(flowTaskTemplateEntity);

    flowTaskTemplateEntity.setLastModified(new Date());
    flowTaskTemplateEntity.setVerified(
        flowTaskTemplateService.getTaskTemplateWithId(flowTaskTemplateEntity.getId()).isVerified());
    return new FlowTaskTemplate(flowTaskTemplateService.updateTaskTemplate(flowTaskTemplateEntity));
  }

  @Override
  public void deleteTaskTemplateWithId(String id) {
    flowTaskTemplateService.deleteTaskTemplate(flowTaskTemplateService.getTaskTemplateWithId(id));
  }

  @Override
  public void activateTaskTemplate(String id) {
    flowTaskTemplateService.activateTaskTemplate(flowTaskTemplateService.getTaskTemplateWithId(id));

  }

  private void updateChangeLog(FlowTaskTemplate flowTaskTemplateEntity) {
    List<Revision> revisions = flowTaskTemplateEntity.getRevisions();
    final FlowUserEntity user = userIdentityService.getCurrentUser();

    if (revisions != null) {
      for (Revision revision : revisions) {
        ChangeLog changelog = revision.getChangelog();
        if (changelog != null && changelog.getUserId() == null) {
          changelog.setUserId(user.getId());
          changelog.setDate(new Date());
          changelog.setUserName(user.getName());
        }
      }
    }
  }

  @Override
  public TektonTask getTaskTemplateYamlWithId(String id) {
    FlowTaskTemplateEntity template = flowTaskTemplateService.getTaskTemplateWithId(id);
    return TetkonConverter.convertFlowTaskToTekton(template, Optional.empty());
  }

  @Override
  public TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber) {
    FlowTaskTemplateEntity template = flowTaskTemplateService.getTaskTemplateWithId(id);
    return TetkonConverter.convertFlowTaskToTekton(template, Optional.of(revisionNumber));
  }

  @Override
  public FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask) {
    FlowTaskTemplateEntity template = TetkonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    template.setStatus(FlowTaskTemplateStatus.active);
    flowTaskTemplateService.insertTaskTemplate(template);
    return new FlowTaskTemplate(template);
  }

  @Override
  public FlowTaskTemplate updateTaskTemplateWuthYaml(String id, TektonTask tektonTask) {
    FlowTaskTemplateEntity tektonTemplate =
        TetkonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    FlowTaskTemplateEntity dbTemplate = flowTaskTemplateService.getTaskTemplateWithId(id);

    if (tektonTemplate.getName() != null && !tektonTemplate.getName().isBlank()) {
      dbTemplate.setName(tektonTemplate.getName());
    }
    if (tektonTemplate.getCategory() != null && !tektonTemplate.getCategory().isBlank()) {
      dbTemplate.setCategory(tektonTemplate.getCategory());
    }

    if (tektonTemplate.getDescription() != null && !tektonTemplate.getDescription().isBlank()) {
      dbTemplate.setDescription(tektonTemplate.getDescription());
    }

    List<Revision> revisions = tektonTemplate.getRevisions();
    if (revisions.size() == 1) {
      Revision revision = revisions.get(0);

      final FlowUserEntity user = userIdentityService.getCurrentUser();
      if (user != null) {
        ChangeLog changelog = revision.getChangelog();
        changelog.setUserId(user.getId());
        changelog.setDate(new Date());
      }


      List<Revision> existingRevisions = dbTemplate.getRevisions();
      int count = existingRevisions.size();
      revision.setVersion(count + 1);
      existingRevisions.add(revision);
    }
    dbTemplate.setLastModified(new Date());
    flowTaskTemplateService.updateTaskTemplate(dbTemplate);
    return this.getTaskTemplateWithId(id);
  }

  @Override
  public FlowTaskTemplate updateTaskTemplateWuthYaml(String id, TektonTask tektonTask,
      Integer revisionId) {
    FlowTaskTemplateEntity tektonTemplate =  TetkonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    FlowTaskTemplateEntity dbTemplate = flowTaskTemplateService.getTaskTemplateWithId(id);
    
    if (tektonTemplate.getName() != null && !tektonTemplate.getName().isBlank()) {
      dbTemplate.setName(tektonTemplate.getName());
    }
    if (tektonTemplate.getCategory() != null && !tektonTemplate.getCategory().isBlank()) {
      dbTemplate.setCategory(tektonTemplate.getCategory());
    }
    
    if (tektonTemplate.getDescription() != null && !tektonTemplate.getDescription().isBlank()) {
      dbTemplate.setDescription(tektonTemplate.getDescription());
    }

    List<Revision> revisions = tektonTemplate.getRevisions();
    if (revisions.size() == 1) {
      Revision revision = revisions.get(0);
      revision.setVersion(revisionId);
      
      final FlowUserEntity user = userIdentityService.getCurrentUser();
      if (user != null) {
        ChangeLog changelog = revision.getChangelog();
        changelog.setUserId(user.getId());
        changelog.setDate(new Date());
      }
      
      List<Revision> existingRevisions = dbTemplate.getRevisions();

      Revision oldRevision = existingRevisions.stream().filter( a -> a.getVersion().equals(revisionId)).findFirst().orElse(null);
      if (oldRevision !=  null) {
        existingRevisions.remove(oldRevision);
        existingRevisions.add(revision);   
      }
    }
    dbTemplate.setLastModified(new Date());
    flowTaskTemplateService.updateTaskTemplate(dbTemplate);
    return this.getTaskTemplateWithId(id);
  }

  @Override
  public List<FlowTaskTemplate> getAllTaskTemplatesForWorkfow(String workflowId) {
    List<FlowTaskTemplate> templates = new LinkedList<>();
    
    WorkflowSummary workflow = this.workflowService.getWorkflow(workflowId);
    String flowTeamId = workflow.getFlowTeamId();
    if (workflow.getScope() == WorkflowScope.team || workflow.getScope() == null) {
        templates = flowTaskTemplateService.getAllTaskTemplatesforTeamId(flowTeamId).stream()
           .map(FlowTaskTemplate::new).collect(Collectors.toList());
    } else if (workflow.getScope() == WorkflowScope.system) {
        templates = flowTaskTemplateService.getAllTaskTemplatesForSystem().stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    }
    
    return templates;
  }
}
