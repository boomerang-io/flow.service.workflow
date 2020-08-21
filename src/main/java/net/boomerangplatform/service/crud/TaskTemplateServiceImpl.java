package net.boomerangplatform.service.crud;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.FlowTaskTemplate;
import net.boomerangplatform.mongo.entity.FlowTaskTemplateEntity;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.mongo.model.ChangeLog;
import net.boomerangplatform.mongo.model.Revision;
import net.boomerangplatform.mongo.service.FlowTaskTemplateService;
import net.boomerangplatform.service.UserIdentityService;

@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {

  @Autowired
  private FlowTaskTemplateService flowTaskTemplateService;

  @Autowired
  private UserIdentityService userIdentityService;

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
  public List<FlowTaskTemplate> getAllTaskTemplates() {
    List<FlowTaskTemplate> templates = flowTaskTemplateService.getAllTaskTemplates().stream()
        .map(FlowTaskTemplate::new).collect(Collectors.toList());

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
    return templates;
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
    flowTaskTemplateEntity.setVerified(flowTaskTemplateService.getTaskTemplateWithId(flowTaskTemplateEntity.getId()).isVerified());
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
}
