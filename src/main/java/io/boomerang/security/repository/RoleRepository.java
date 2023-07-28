package io.boomerang.security.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.security.entity.RoleEntity;

public interface RoleRepository extends MongoRepository<RoleEntity, String> {
  
  List<RoleEntity> findByType(String type);
  
  RoleEntity findByTypeAndName(String type, String name);
  
}
