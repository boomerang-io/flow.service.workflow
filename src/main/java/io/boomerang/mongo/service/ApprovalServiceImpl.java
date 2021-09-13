package io.boomerang.mongo.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import io.boomerang.model.ApprovalStatus;
import io.boomerang.mongo.entity.ApprovalEntity;
import io.boomerang.mongo.model.ManualType;
import io.boomerang.mongo.repository.FlowApprovalRepository;

@Service
public class ApprovalServiceImpl implements ApprovalService {

  @Autowired
  private FlowApprovalRepository flowRepository;
  
  @Autowired
  private MongoTemplate mongoTemplate;
  
  @Override
  public List<ApprovalEntity> getActiivtyForTeam(String flowTeamId) {
    return flowRepository.findByTeamId(flowTeamId);
  }

  @Override
  public ApprovalEntity save(ApprovalEntity approval) {
    return flowRepository.save(approval);
  }

  @Override
  public ApprovalEntity findById(String id) {
    return flowRepository.findById(id).orElse(null);
  }

  @Override
  public ApprovalEntity findByTaskActivityId(String id) {
    return flowRepository.findByTaskActivityId(id);
  }

  @Override
  public long getApprovalCountForActivity(String activityId, ApprovalStatus status) {
    return flowRepository.countByActivityIdAndStatus(activityId, status);
  }

  @Override
  public Page<ApprovalEntity> getAllApprovals(Optional<Date> from, Optional<Date> to,
      Pageable pageable, List<String> workflowIds, Optional<ManualType> type, Optional<ApprovalStatus> status) {
    
    Criteria criteria = extracted(from, to, workflowIds, type, status);
    Query approvalQuery = new Query(criteria).with(pageable);
    List<ApprovalEntity> list = this.mongoTemplate.find(approvalQuery, ApprovalEntity.class);
    Page<ApprovalEntity> paginaedApprovalList = new PageImpl<>(list, pageable, mongoTemplate.count(new Query(criteria), ApprovalEntity.class));
    return paginaedApprovalList;
  }

  private Criteria extracted(Optional<Date> from, Optional<Date> to, List<String> workflowIds,
      Optional<ManualType> type, Optional<ApprovalStatus> status) {
    List<Criteria> criterias = new ArrayList<>();
    
    if (from.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("creationDate").gte(from.get());
      criterias.add(dynamicCriteria);
    }
    
    if (to.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("creationDate").lte(to.get());
      criterias.add(dynamicCriteria);
    }
    
    if (type.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("type").is(type.get());
      criterias.add(dynamicCriteria);
    }
    
    if (status.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("status").is(status.get());
      criterias.add(dynamicCriteria);
    }
    
    Criteria workflowIdsCriteria = Criteria.where("workflowId").in(workflowIds);
    criterias.add(workflowIdsCriteria);
    Criteria criteria = new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
    return criteria;
  }

  @Override
  public long getActionCountForType(ManualType type,  Optional<Date> from, Optional<Date> to) {
    if (from.isPresent() && to.isPresent()) {
      return flowRepository.countByStatusAndTypeAndCreationDateBetween(ApprovalStatus.submitted, type, from.get(), to.get());
    }
    return flowRepository.countByStatusAndType(ApprovalStatus.submitted,type);
  }

  @Override
  public long getActionCount(Optional<Date> from,  Optional<Date> to) {
    if (from.isPresent() && to.isPresent()) { 
      return flowRepository.countByStatusAndCreationDateBetween(ApprovalStatus.submitted, from.get(), to.get());
    }
    return flowRepository.countByStatus(ApprovalStatus.submitted);
  }

  @Override
  public long getActionCountForStatus(ApprovalStatus status, Optional<Date> from,
      Optional<Date> to) {
    if (from.isPresent() && to.isPresent()) { 
      return flowRepository.countByStatusAndCreationDateBetween(status, from.get(), to.get());
    }
    return flowRepository.countByStatus(status);
  }
}
