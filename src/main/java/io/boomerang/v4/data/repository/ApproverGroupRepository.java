package io.boomerang.v4.data.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.ApproverGroupEntity;

public interface ApproverGroupRepository extends MongoRepository<ApproverGroupEntity, String> {
  
  List<ApproverGroupEntity> findByIdIn(List<String> ids);
  
}
