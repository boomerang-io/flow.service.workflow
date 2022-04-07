package io.boomerang.mongo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.mongo.entity.TeamEntity;

public interface FlowTeamRepository extends MongoRepository<TeamEntity, String> {
  List<TeamEntity> findByhigherLevelGroupIdIn(List<String> ids);

  List<TeamEntity> findByNameLikeIgnoreCase(String name);

  @Override
  Optional<TeamEntity> findById(String id);

  Page<TeamEntity> findByIsActive(Pageable pageable, boolean b);
}
