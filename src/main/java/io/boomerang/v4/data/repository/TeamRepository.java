package io.boomerang.v4.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.TeamEntity;

public interface TeamRepository extends MongoRepository<TeamEntity, String> {
  List<TeamEntity> findByhigherLevelGroupIdIn(List<String> ids);

  List<TeamEntity> findByNameLikeIgnoreCase(String name);

  @Override
  Optional<TeamEntity> findById(String id);

  List<TeamEntity> findByIdIn(List<String> ids);

  Page<TeamEntity> findByIsActive(Pageable pageable, boolean b);

  List<TeamEntity> findByIdInAndIsActive(List<String> ids, Boolean isActive);
}
