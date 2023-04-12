package io.boomerang.v4.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;

public interface RelationshipRepository extends MongoRepository<RelationshipEntity, String> {

//  Optional<RelationshipEntity> findByFromTypeAndFromRef(RelationshipRef fromType, String fromRef);
//
  Optional<RelationshipEntity> findByFromTypeAndFromRefAndRelationship(RelationshipRef fromType, String fromRef, RelationshipType relationship);
//
//  List<RelationshipEntity> findByFromTypeAndFromRefAndToType(RelationshipRef fromType, String fromRef, RelationshipRef toType);
//
//  List<RelationshipEntity> findByFromTypeAndFromRefAndRelationshipAndToType(RelationshipRef fromType, String fromRef, RelationshipType relationship, RelationshipRef toType);
//
//  List<RelationshipEntity> findByFromTypeAndToType(RelationshipRef fromType, RelationshipRef toType);
//  
//  List<RelationshipEntity> findByFromTypeAndRelationship(RelationshipRef fromType, RelationshipType relationship);
//  
  List<RelationshipEntity> findByFromTypeAndRelationshipAndToType(RelationshipRef fromType, RelationshipType relationship, RelationshipRef toType);
//
//  List<RelationshipEntity> findByFromTypeAndToTypeAndToRef(RelationshipRef fromType, RelationshipRef toType,
//      String toRef);
//
//  List<RelationshipEntity> findByFromTypeAndToTypeAndToRefIn(RelationshipRef fromType, RelationshipRef toType,
//      List<String> toRef);
  
List<RelationshipEntity> findByFromTypeAndFromRefInAndRelationshipAndToType(RelationshipRef fromType, List<String> fromRef, RelationshipType relationship, RelationshipRef toType);
  
List<RelationshipEntity> findByFromTypeAndRelationshipAndToTypeAndToRefIn(RelationshipRef fromType, RelationshipType relationship, RelationshipRef toType,
    List<String> toRef);

  List<RelationshipEntity> findByFromTypeAndFromRefInAndToTypeAndToRefIn(RelationshipRef fromType, List<String> fromRef, RelationshipRef toType,
      List<String> toRef);

  List<RelationshipEntity> findByFromTypeAndFromRefInAndRelationshipAndToTypeAndToRefIn(RelationshipRef fromType, List<String> fromRef, RelationshipType relationship, RelationshipRef toType,
      List<String> toRef);
}
