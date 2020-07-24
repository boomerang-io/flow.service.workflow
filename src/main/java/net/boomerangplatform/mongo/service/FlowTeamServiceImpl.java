package net.boomerangplatform.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;
import net.boomerangplatform.mongo.repository.FlowTeamRepository;

@Service
public class FlowTeamServiceImpl implements FlowTeamService {

  @Autowired
  private FlowTeamRepository flowTeamRepository;

  @Override
  public Page<FlowTeamEntity> findAllTeams(Pageable pageable) {
    return flowTeamRepository.findAll(pageable);
  }

  @Override
  public List<FlowTeamEntity> findTeamsWithHighLevelGroups(List<String> highLevelGroups) {
    return flowTeamRepository.findByhigherLevelGroupIdIn(highLevelGroups);
  }

  @Override
  public FlowTeamEntity save(FlowTeamEntity entity) {
    return flowTeamRepository.save(entity);
  }

  @Override
  public FlowTeamEntity findById(String id) {
    return flowTeamRepository.findById(id).orElse(null);

  }

}
