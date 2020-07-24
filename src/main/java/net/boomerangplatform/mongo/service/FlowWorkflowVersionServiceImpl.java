package net.boomerangplatform.mongo.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;
import net.boomerangplatform.mongo.repository.FlowWorkflowVersionRepository;

@Service
public class FlowWorkflowVersionServiceImpl implements FlowWorkflowVersionService {

  @Autowired
  private FlowWorkflowVersionRepository workFlowVersionRepository;

  @Override
  @NoLogging
  public void deleteWorkflow(FlowWorkflowRevisionEntity flowWorkflowVersionEntity) {
    workFlowVersionRepository.delete(flowWorkflowVersionEntity);
  }

  @Override
  public FlowWorkflowRevisionEntity getLatestWorkflowVersion(String workflowId) {
    final long versionCount = workFlowVersionRepository.countByworkFlowId(workflowId);
    return workFlowVersionRepository.findByworkFlowIdAndVersion(workflowId, versionCount);
  }

  @Override
  public FlowWorkflowRevisionEntity getLatestWorkflowVersion(String workflowId, long version) {
    return workFlowVersionRepository.findByworkFlowIdAndVersion(workflowId, version);
  }

  @Override
  public long getWorkflowCount(String workFlowId) {
    return this.workFlowVersionRepository.countByworkFlowId(workFlowId);
  }

  @Override
  @NoLogging
  public FlowWorkflowRevisionEntity getWorkflowlWithId(String id) {
    return workFlowVersionRepository.findById(id).orElse(null);
  }

  @Override
  @NoLogging
  public FlowWorkflowRevisionEntity insertWorkflow(
      FlowWorkflowRevisionEntity flowWorkflowVersionEntity) {
    return workFlowVersionRepository.insert(flowWorkflowVersionEntity);
  }

  @Override
  @NoLogging
  public FlowWorkflowRevisionEntity updateWorkflow(
      FlowWorkflowRevisionEntity flowWorkflowVersionEntity) {
    return workFlowVersionRepository.save(flowWorkflowVersionEntity);
  }


  @Override
  public Page<FlowWorkflowRevisionEntity> getAllWorkflowVersions(Optional<String> workFlowId,
      Pageable pageable) {
    if (workFlowId.isPresent()) {
      return workFlowVersionRepository.findByworkFlowId(workFlowId.get(), pageable);
    } else {
      return workFlowVersionRepository.findAll(pageable);
    }
  }
}
