package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.TeamEntity;

public interface TeamRepository extends MongoRepository<TeamEntity, String> {

  Optional<TeamEntity> findByNameIgnoreCase(String name);
  
  Long countByNameIgnoreCase(String name);

  @Override
  Optional<TeamEntity> findById(String id);

  List<TeamEntity> findByIdIn(List<String> ids);
  
  void deleteByName(String name);
}
