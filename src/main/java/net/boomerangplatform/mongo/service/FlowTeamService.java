package net.boomerangplatform.mongo.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;

public interface FlowTeamService {

  Page<FlowTeamEntity> findAllTeams(Pageable pageable);

  List<FlowTeamEntity> findTeamsWithHighLevelGroups(List<String> highLevelGroups);

  FlowTeamEntity save(FlowTeamEntity entity);

  FlowTeamEntity findById(String id);

}
