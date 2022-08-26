package io.boomerang.mongo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.WorkFlowRevisionCount;
import io.boomerang.mongo.repository.FlowWorkflowVersionRepository;

@Service
public class RevisionServiceImpl implements RevisionService {

  @Autowired
  private FlowWorkflowVersionRepository workFlowVersionRepository;

  @Override
  @NoLogging
  public void deleteWorkflow(RevisionEntity flowWorkflowVersionEntity) {
    workFlowVersionRepository.delete(flowWorkflowVersionEntity);
  }

  @Override
  public RevisionEntity getLatestWorkflowVersion(String workflowId) {
    final long versionCount = workFlowVersionRepository.countByworkFlowId(workflowId);
    return workFlowVersionRepository.findByworkFlowIdAndVersion(workflowId, versionCount);
  }

  @Override
  public RevisionEntity getLatestWorkflowVersion(String workflowId, long version) {
    return workFlowVersionRepository.findByworkFlowIdAndVersion(workflowId, version);
  }

  @Override
  public long getWorkflowCount(String workFlowId) {
    return this.workFlowVersionRepository.countByworkFlowId(workFlowId);
  }

  @Override
  @NoLogging
  public RevisionEntity getWorkflowlWithId(String id) {
    return workFlowVersionRepository.findById(id).orElse(null);
  }

  @Override
  @NoLogging
  public RevisionEntity insertWorkflow(RevisionEntity flowWorkflowVersionEntity) {
    return workFlowVersionRepository.insert(flowWorkflowVersionEntity);
  }

  @Override
  @NoLogging
  public RevisionEntity updateWorkflow(RevisionEntity flowWorkflowVersionEntity) {
    return workFlowVersionRepository.save(flowWorkflowVersionEntity);
  }


  @Override
  public Page<RevisionEntity> getAllWorkflowVersions(Optional<String> workFlowId,
      Pageable pageable) {
    if (workFlowId.isPresent()) {
      return workFlowVersionRepository.findByworkFlowId(workFlowId.get(), pageable);
    } else {
      return workFlowVersionRepository.findAll(pageable);
    }
  }

  @Override
  public Optional<RevisionEntity> getRevision(String id) {
    return workFlowVersionRepository.findById(id);
  }

  @Override
  public RevisionEntity findRevisionTaskProperty(String workflowId, long workflowVersion, String taskId,
      String propertyKey) {
    return workFlowVersionRepository
        .findByworkFlowIdAndVersionAndDagTasksTaskIdAndDagTasksPropertiesKey(workflowId,
            workflowVersion, taskId, propertyKey);
  }
  
  @Override
  public List<WorkFlowRevisionCount> getWorkflowRevisionCounts(List<String> workFlowIds) {
  	return workFlowVersionRepository.findWorkFlowVersionCounts(workFlowIds);
  }

  @Override
  public List<WorkFlowRevisionCount> getWorkflowRevisionCountsAndLatestVersion(List<String> workFlowIds) {
  	return workFlowVersionRepository.findWorkFlowVersionCountsAndLatestVersion(workFlowIds);
  }
}
