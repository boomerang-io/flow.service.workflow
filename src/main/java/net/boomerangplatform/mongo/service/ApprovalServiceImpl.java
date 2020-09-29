package net.boomerangplatform.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.ApprovalEntity;
import net.boomerangplatform.mongo.repository.FlowApprovalRepository;

@Service
public class ApprovalServiceImpl implements ApprovalService {

  @Autowired
  private FlowApprovalRepository flowRepository;
  
  @Override
  public List<ApprovalEntity> getActiivtyForTeam(String flowTeamId) {
    return flowRepository.findByTeamId(flowTeamId);
  }

  @Override
  public ApprovalEntity createApproval(ApprovalEntity approval) {
    return flowRepository.save(approval);
  }

  @Override
  public ApprovalEntity findById(String id) {
    return flowRepository.findById(id).orElse(null);
  }  
}
