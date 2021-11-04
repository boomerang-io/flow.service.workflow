package io.boomerang.service.crud;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import io.boomerang.model.FlowTaskTemplate;
import io.boomerang.model.TemplateScope;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.model.tekton.TektonTask;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.ChangeLog;
import io.boomerang.mongo.model.FlowTaskTemplateStatus;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.UserType;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.service.UserIdentityService;
import io.boomerang.service.tekton.TektonConverter;

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
      templates = flowTaskTemplateService.getAllGlobalTasks().stream().map(FlowTaskTemplate::new)
          .collect(Collectors.toList());
    } else if (scope == TemplateScope.team) {
      templates = flowTaskTemplateService.getTaskTemplatesforTeamId(teamId).stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    } else if (scope == TemplateScope.system) {
      templates = flowTaskTemplateService.getAllSystemTasks().stream().map(FlowTaskTemplate::new)
          .collect(Collectors.toList());
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
    FlowUserEntity user = userIdentityService.getCurrentUser();

    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {

      Date creationDate = new Date();

      flowTaskTemplateEntity.setCreatedDate(creationDate);
      flowTaskTemplateEntity.setLastModified(creationDate);
      flowTaskTemplateEntity.setVerified(false);

      updateChangeLog(flowTaskTemplateEntity);

      return new FlowTaskTemplate(
          flowTaskTemplateService.insertTaskTemplate(flowTaskTemplateEntity));

    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

  }

  @Override
  public FlowTaskTemplate updateTaskTemplate(FlowTaskTemplate flowTaskTemplateEntity) {
    FlowUserEntity user = userIdentityService.getCurrentUser();

    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {

      updateChangeLog(flowTaskTemplateEntity);

      flowTaskTemplateEntity.setLastModified(new Date());
      flowTaskTemplateEntity.setVerified(flowTaskTemplateService
          .getTaskTemplateWithId(flowTaskTemplateEntity.getId()).isVerified());
      return new FlowTaskTemplate(
          flowTaskTemplateService.updateTaskTemplate(flowTaskTemplateEntity));

    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  @Override
  public void deleteTaskTemplateWithId(String id) {
    FlowUserEntity user = userIdentityService.getCurrentUser();

    if (user.getType() == UserType.admin || user.getType() == UserType.operator) {
      flowTaskTemplateService.deleteTaskTemplate(flowTaskTemplateService.getTaskTemplateWithId(id));
    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
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
    return TektonConverter.convertFlowTaskToTekton(template, Optional.empty());
  }

  @Override
  public TektonTask getTaskTemplateYamlWithIdAndRevision(String id, Integer revisionNumber) {
    FlowTaskTemplateEntity template = flowTaskTemplateService.getTaskTemplateWithId(id);
    return TektonConverter.convertFlowTaskToTekton(template, Optional.of(revisionNumber));
  }

  @Override
  public FlowTaskTemplate insertTaskTemplateYaml(TektonTask tektonTask, TemplateScope scope,
      String teamId) {
    FlowTaskTemplateEntity template = TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    template.setStatus(FlowTaskTemplateStatus.active);
    template.setScope(scope);
    template.setFlowTeamId(teamId);

    flowTaskTemplateService.insertTaskTemplate(template);
    return new FlowTaskTemplate(template);
  }

  @Override
  public FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask) {
    FlowTaskTemplateEntity tektonTemplate =
        TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
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
  public FlowTaskTemplate updateTaskTemplateWithYaml(String id, TektonTask tektonTask,
      Integer revisionId, String comment) {
    FlowTaskTemplateEntity tektonTemplate =
        TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
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
        changelog.setReason(comment);
      }

      List<Revision> existingRevisions = dbTemplate.getRevisions();

      Revision oldRevision = existingRevisions.stream()
          .filter(a -> a.getVersion().equals(revisionId)).findFirst().orElse(null);
      if (oldRevision != null) {
        existingRevisions.remove(oldRevision);

      }
      existingRevisions.add(revision);
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
    } else if (workflow.getScope() == WorkflowScope.system || workflow.getScope() == WorkflowScope.user) {
      templates = flowTaskTemplateService.getAllTaskTemplatesForSystem().stream()
          .map(FlowTaskTemplate::new).collect(Collectors.toList());
    }

    return templates;
  }

  @Override
  public FlowTaskTemplate validateTaskTemplate(TektonTask tektonTask) {
    FlowTaskTemplateEntity template = TektonConverter.convertTektonTaskToNewFlowTask(tektonTask);
    template.setStatus(FlowTaskTemplateStatus.active);
    return new FlowTaskTemplate(template);
  }
}
