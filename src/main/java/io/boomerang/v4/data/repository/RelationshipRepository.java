package io.boomerang.v4.data.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.RelationshipEntity;

public interface RelationshipRepository
    extends MongoRepository<RelationshipEntity, String> {
  
  List<RelationshipEntity> findByFromTypeAndToType(String fromType, String toType);
  
    List<RelationshipEntity> findByFromTypeAndToTypeAndToRef(String fromType, String toType, String toRef);
  
    List<RelationshipEntity> findByFromTypeAndToTypeAndToRefIn(String fromType, String toType, List<String> toRef);
}