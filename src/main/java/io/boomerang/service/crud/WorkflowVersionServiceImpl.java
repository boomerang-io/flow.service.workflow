package io.boomerang.service.crud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import io.boomerang.model.FlowWorkflowRevision;
import io.boomerang.model.RevisionResponse;
import io.boomerang.model.projectstormv5.ConfigNodes;
import io.boomerang.model.projectstormv5.RestConfig;
import io.boomerang.model.projectstormv5.TaskNode;
import io.boomerang.mongo.entity.FlowTaskTemplateEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.ChangeLog;
import io.boomerang.mongo.model.Dag;
import io.boomerang.mongo.model.Revision;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.UserIdentityService;

@Service
public class WorkflowVersionServiceImpl implements WorkflowVersionService {

  @Autowired
  private RevisionService flowWorkflowService;

  @Autowired
  private UserIdentityService userIdentityService;

  @Autowired
  private FlowTaskTemplateService templateService;

  @Autowired
  private FlowWorkflowService workFlowRepository;

  @Override
  public void deleteWorkflowVersionWithId(String id) {
    flowWorkflowService.deleteWorkflow(flowWorkflowService.getWorkflowlWithId(id));
  }

  @Override
  public FlowWorkflowRevision getLatestWorkflowVersion(String workflowId) {

//    final WorkflowEntity entity = workFlowRepository.getWorkflow(workflowId);

//    if (entity.getScope() == WorkflowScope.user
//        && !entity.getOwnerUserId().equals(userIdentityService.getCurrentUser().getId())) {
//      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
//    }

    RevisionEntity revision = flowWorkflowService.getLatestWorkflowVersion(workflowId);
    if (revision == null) {
      return null;
    }

    updateTemplateVersions(revision);

    FlowWorkflowRevision flowRevision = new FlowWorkflowRevision(revision);
    updateUpgradeFlags(flowRevision);


    return flowRevision;
  }

  @Override
  public long getLatestWorkflowVersionCount(String workflowId) {
    return flowWorkflowService.getWorkflowCount(workflowId);

  }

  @Override
  public FlowWorkflowRevision getWorkflowVersion(String workflowId, long verison) {

    final WorkflowEntity entity = workFlowRepository.getWorkflow(workflowId);

    if (entity.getScope() == WorkflowScope.user
        && !entity.getOwnerUserId().equals(userIdentityService.getCurrentUser().getId())) {
      throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
    }
    RevisionEntity revision = flowWorkflowService.getLatestWorkflowVersion(workflowId, verison);

    updateTemplateVersions(revision);

    FlowWorkflowRevision flowRevision = new FlowWorkflowRevision(revision);
    updateUpgradeFlags(flowRevision);

    return flowRevision;
  }

  private void updateTemplateVersions(RevisionEntity revision) { // NOSONAR
    Dag dag = revision.getDag();
    if (dag != null) {
      List<DAGTask> dagTasks = dag.getTasks();
      for (DAGTask dagTask : dagTasks) {
        if (dagTask.getTemplateVersion() == null && dagTask.getTemplateId() != null) {
          FlowTaskTemplateEntity flowTaskTemplateEntity =
              templateService.getTaskTemplateWithId(dagTask.getTemplateId());
          if (flowTaskTemplateEntity != null && flowTaskTemplateEntity.getRevisions() != null) {
            Optional<Revision> latestRevision = flowTaskTemplateEntity.getRevisions().stream()
                .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
            if (latestRevision.isPresent()) {
              Integer newVersion = latestRevision.get().getVersion();
              dagTask.setTemplateVersion(newVersion);
            }
          }
        }
      }
    }
  }


  private boolean isTask(String type) {
    return ("templateTask".equals(type) || "customTask".equals(type) || "decision".equals(type));
  }

  private void updateUpgradeFlags(FlowWorkflowRevision revision) { // NOSONAR

    boolean newTemplatesAvailable = false;

    if (revision.getConfig() != null) {

      RestConfig configList = revision.getConfig();

      if (configList != null) {
        for (ConfigNodes config : configList.getNodes()) {
          if (isTask(config.getType())) {

            Integer taskVersion = config.getTaskVersion();
            FlowTaskTemplateEntity flowTaskTemplateEntity =
                templateService.getTaskTemplateWithId(config.getTaskId());

            if (flowTaskTemplateEntity != null && flowTaskTemplateEntity.getRevisions() != null) {
              Optional<Revision> latestRevision = flowTaskTemplateEntity.getRevisions().stream()
                  .sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
              if (latestRevision.isPresent()
                  && !latestRevision.get().getVersion().equals(taskVersion)) {
                newTemplatesAvailable = true;
                if (revision.getDag().getNodes() != null) {
                  for (TaskNode taskNode : revision.getDag().getNodes()) {
                    if (taskNode.getNodeId() != null && config.getNodeId() != null
                        && taskNode.getNodeId().equals(config.getNodeId())) {
                      taskNode.setTemplateUpgradeAvailable(true);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    revision.setTemplateUpgradesAvailable(newTemplatesAvailable);
  }

  @Override
  public FlowWorkflowRevision getWorkflowVersionWithId(String id) {
    return new FlowWorkflowRevision(flowWorkflowService.getWorkflowlWithId(id));
  }

  @Override
  public FlowWorkflowRevision insertWorkflowVersion(FlowWorkflowRevision flowWorkflowEntity) {

    final String workFlowId = flowWorkflowEntity.getWorkFlowId();
    final long currentCount = flowWorkflowService.getWorkflowCount(workFlowId);

    final ChangeLog changelog = new ChangeLog();
    FlowUserEntity user = userIdentityService.getCurrentUser();

    changelog.setUserId(user.getId());

    if (flowWorkflowEntity.getChangelog() != null) {
      final String reason = flowWorkflowEntity.getChangelog().getReason();
      changelog.setReason(reason);
    }

    changelog.setDate(new Date());

    flowWorkflowEntity.setChangelog(changelog);
    flowWorkflowEntity.setId(null);
    flowWorkflowEntity.setVersion(currentCount + 1);
    RevisionEntity revisionEntity = flowWorkflowEntity.convertToEntity();
    this.updateTemplateVersions(revisionEntity);

    FlowWorkflowRevision newRevision = new FlowWorkflowRevision(
        flowWorkflowService.insertWorkflow(flowWorkflowEntity.convertToEntity()));

    updateUpgradeFlags(newRevision);
    return newRevision;
  }

  @Override
  public List<RevisionResponse> viewChangelog(Optional<String> workFlowId, Pageable pageable) {
    final Page<RevisionEntity> revisions =
        flowWorkflowService.getAllWorkflowVersions(workFlowId, pageable);

    final List<RevisionResponse> revisionResponse = new ArrayList<>();

    for (final RevisionEntity revision : revisions) {
      final RevisionResponse rs = new RevisionResponse();
      String userId;
      String userName;

      rs.setRevisionId(revision.getId());
      rs.setWorkflowId(revision.getWorkFlowId());
      rs.setVersion(revision.getVersion());

      if (revision.getChangelog() != null) {

        if (revision.getChangelog().getUserId() != null) {
          userId = revision.getChangelog().getUserId();
        } else {
          userId = null;
        }

        final Date date = revision.getChangelog().getDate();
        final String reason = revision.getChangelog().getReason();

        rs.setDate(date);
        rs.setUserId(userId);
        rs.setReason(reason);

        if (userIdentityService.getUserByID(userId) != null) {
          userName = userIdentityService.getUserByID(userId).getName();
        } else {
          userName = null;
        }

        rs.setUserName(userName);
      }

      revisionResponse.add(rs);
    }
    return revisionResponse;
  }
}
