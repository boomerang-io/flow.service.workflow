package io.boomerang.data.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.GlobalParamEntity;

public interface GlobalParamRepository
    extends MongoRepository<GlobalParamEntity, String> {

  @Override
  Optional<GlobalParamEntity> findById(String id);

  Optional<GlobalParamEntity> findOneByKey(String key);

  void deleteByKey(String key);
  
  Integer countByKey(String key);

}
