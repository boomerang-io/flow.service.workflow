package io.boomerang.v4.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;

public interface RelationshipRepository extends MongoRepository<RelationshipEntity, String> {
//
  Optional<RelationshipEntity> findByFromAndFromRefAndType(RelationshipRef from, String fromRef, RelationshipType type);
//  
  List<RelationshipEntity> findByFromAndTypeAndTo(RelationshipRef from, RelationshipType type, RelationshipRef to);
  
List<RelationshipEntity> findByFromAndFromRefInAndTypeAndTo(RelationshipRef from, List<String> fromRef, RelationshipType type, RelationshipRef to);
  
List<RelationshipEntity> findByFromAndTypeAndToAndToRefIn(RelationshipRef from, RelationshipType type, RelationshipRef to,
    List<String> toRef);

  List<RelationshipEntity> findByFromAndFromRefInAndToAndToRefIn(RelationshipRef from, List<String> fromRef, RelationshipRef to,
      List<String> toRef);

  List<RelationshipEntity> findByFromAndFromRefInAndTypeAndToAndToRefIn(RelationshipRef from, List<String> fromRef, RelationshipType type, RelationshipRef to,
      List<String> toRef);
}
