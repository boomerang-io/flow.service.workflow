package io.boomerang.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.repository.FlowTeamRepository;

@Service
public class FlowTeamServiceImpl implements FlowTeamService {

  @Autowired
  private FlowTeamRepository flowTeamRepository;

  @Override
  public Page<TeamEntity> findAllActiveTeams(Pageable pageable) {
    return flowTeamRepository.findByIsActive(pageable,true);
  }
  
  @Override
  public Page<TeamEntity> findAllTeams(Pageable pageable) {
    return flowTeamRepository.findAll(pageable);
  }

  @Override
  public List<TeamEntity> findActiveTeamsByIds(List<String> ids) {
    return flowTeamRepository.findByIdInAndIsActive(ids, true);
  }

  @Override
  public List<TeamEntity> findActiveTeamsByIds(List<String> ids) {
    return flowTeamRepository.findByIdInAndIsActive(ids, true);
  }

  @Override
  public TeamEntity save(TeamEntity entity) {
    return flowTeamRepository.save(entity);
  }

  @Override
  public TeamEntity findById(String id) {
    return flowTeamRepository.findById(id).orElse(null);

  }

}
