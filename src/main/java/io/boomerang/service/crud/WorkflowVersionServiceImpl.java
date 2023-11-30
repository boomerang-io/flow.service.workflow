package io.boomerang.service.crud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import com.google.inject.internal.util.Lists;
import com.google.inject.internal.util.Maps;

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
import io.boomerang.mongo.model.WorkFlowRevisionCount;
import io.boomerang.mongo.model.next.DAGTask;
import io.boomerang.mongo.service.FlowTaskTemplateService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.security.service.UserValidationService;
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
  
  @Autowired
  private UserValidationService userValidationService;

  @Override
  public void deleteWorkflowVersionWithId(String id) {
    flowWorkflowService.deleteWorkflow(flowWorkflowService.getWorkflowlWithId(id));
  }

  @Override
  public FlowWorkflowRevision getLatestWorkflowVersion(String workflowId) {

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
    try {
      userValidationService.validateUserAccessForWorkflow(entity.getScope(),
    			entity.getFlowTeamId(), entity.getOwnerUserId(), false);
    } catch (ResponseStatusException e) {
      throw new HttpClientErrorException(e.getStatus());
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
  
  @Override
  public List<FlowWorkflowRevision> getLatestWorkflowVersionWithUpgradeFlags(List<String> workflowIds) {
		List<WorkFlowRevisionCount> workFlowLatestRevisions = flowWorkflowService.getWorkflowRevisionCountsAndLatestVersion(workflowIds);
		List<FlowWorkflowRevision> latestFlowRevisionList = Lists.newArrayList();
		workFlowLatestRevisions.stream().forEach(workFlowLatestRevision->{
			latestFlowRevisionList.add(new FlowWorkflowRevision(workFlowLatestRevision.getLatestVersion()));
		});
	
		// Batch updating upgrade available flags
		setTemplateUpgradeFlags(latestFlowRevisionList);  
	  return latestFlowRevisionList;
	}

	private void setTemplateUpgradeFlags(List<FlowWorkflowRevision> latestFlowRevisionList) {
		// Collect Task IDs
		List<String> taskIds = new ArrayList<String>();
		for (FlowWorkflowRevision latestFlowRevision : latestFlowRevisionList) {
			if (latestFlowRevision.getConfig() == null) {
				continue;
			}
			for (ConfigNodes config : latestFlowRevision.getConfig().getNodes()) {
				if (isTask(config.getType())) {
					taskIds.add(config.getTaskId());
				}
			}
		}

		// Batch Query FlowTaskTemplateEntity
		List<FlowTaskTemplateEntity> flowTaskTemplateEntities = templateService.getTaskTemplateWithIds(taskIds);
		Map<String, FlowTaskTemplateEntity> flowTaskTemplateEntitiesMap = Maps.newHashMap();
		flowTaskTemplateEntities.stream().forEach(flowTaskTemplateEntity -> {
			flowTaskTemplateEntitiesMap.put(flowTaskTemplateEntity.getId(), flowTaskTemplateEntity);
		});

		// Setting template upgrade flags
		for (FlowWorkflowRevision latestFlowRevision : latestFlowRevisionList) {
			this.setTemplateUpgradeFlags(latestFlowRevision, flowTaskTemplateEntitiesMap);
		}
	}
	
	private void setTemplateUpgradeFlags(FlowWorkflowRevision latestFlowRevision, Map<String, FlowTaskTemplateEntity> flowTaskTemplateEntitiesMap) {
		latestFlowRevision.setTemplateUpgradesAvailable(false);
		if (latestFlowRevision.getConfig() == null) {
			return;
		}
		
		for (ConfigNodes config : latestFlowRevision.getConfig().getNodes()) {
			this.setTemplateUpgradeFlags(latestFlowRevision, config, flowTaskTemplateEntitiesMap);
		}
	}
	
	private void setTemplateUpgradeFlags(FlowWorkflowRevision latestFlowRevision, ConfigNodes configNodes, Map<String, FlowTaskTemplateEntity> flowTaskTemplateEntitiesMap) {
		if(!isTask(configNodes.getType()) || flowTaskTemplateEntitiesMap.get(configNodes.getTaskId()) == null
				|| flowTaskTemplateEntitiesMap.get(configNodes.getTaskId()).getRevisions() == null) {
			return;
		}
		
		Optional<Revision> latestRevision = flowTaskTemplateEntitiesMap.get(configNodes.getTaskId()).getRevisions()
				.stream().sorted(Comparator.comparingInt(Revision::getVersion).reversed()).findFirst();
		Integer taskVersion = configNodes.getTaskVersion();
		if(!latestRevision.isPresent() || latestRevision.get().getVersion().equals(taskVersion)) {
				return;
		}
		latestFlowRevision.setTemplateUpgradesAvailable(true);
		if (latestFlowRevision.getDag().getNodes() == null) {
			return;
		}
		for (TaskNode taskNode : latestFlowRevision.getDag().getNodes()) {
			if (taskNode.getNodeId() != null && configNodes.getNodeId() != null
					&& taskNode.getNodeId().equals(configNodes.getNodeId())) {
				taskNode.setTemplateUpgradeAvailable(true);
			}
		}
	}

}
