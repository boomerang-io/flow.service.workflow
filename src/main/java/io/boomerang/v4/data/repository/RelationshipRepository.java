package io.boomerang.v4.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.model.enums.RelationshipRefType;

public interface RelationshipRepository extends MongoRepository<RelationshipEntity, String> {

  Optional<RelationshipEntity> findByFromTypeAndFromRef(RelationshipRefType fromType, String fromRef);

  List<RelationshipEntity> findByFromTypeAndFromRefAndToType(RelationshipRefType fromType, String fromRef, RelationshipRefType toType);

  List<RelationshipEntity> findByFromTypeAndToType(RelationshipRefType fromType, RelationshipRefType toType);

  List<RelationshipEntity> findByFromTypeAndToTypeAndToRef(RelationshipRefType fromType, RelationshipRefType toType,
      String toRef);

  List<RelationshipEntity> findByFromTypeAndToTypeAndToRefIn(RelationshipRefType fromType, RelationshipRefType toType,
      List<String> toRef);

  List<RelationshipEntity> findByFromTypeAndFromRefInAndToTypeAndToRefIn(RelationshipRefType fromType, List<String> fromRef, RelationshipRefType toType,
      List<String> toRef);
}
