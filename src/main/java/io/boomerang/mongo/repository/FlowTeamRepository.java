package io.boomerang.mongo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.mongo.entity.FlowTeamEntity;

public interface FlowTeamRepository extends MongoRepository<FlowTeamEntity, String> {
  List<FlowTeamEntity> findByhigherLevelGroupIdIn(List<String> ids);

  List<FlowTeamEntity> findByNameLikeIgnoreCase(String name);

  @Override
  Optional<FlowTeamEntity> findById(String id);

  Page<FlowTeamEntity> findByIsActive(Pageable pageable, boolean b);
}
