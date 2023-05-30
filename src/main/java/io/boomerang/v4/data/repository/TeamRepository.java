package io.boomerang.v4.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.TeamEntity;

public interface TeamRepository extends MongoRepository<TeamEntity, String> {

  List<TeamEntity> findByNameLikeIgnoreCase(String name);
  
  Long countByNameIgnoreCase(String name);

  @Override
  Optional<TeamEntity> findById(String id);

  List<TeamEntity> findByIdIn(List<String> ids);
}
