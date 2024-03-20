package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Update;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;

public interface RelationshipRepository extends MongoRepository<RelationshipEntity, String> {
//
  Optional<RelationshipEntity> findByFromAndFromRefAndType(RelationshipType from, String fromRef, RelationshipLabel type);
//  
  List<RelationshipEntity> findByFromAndTypeAndTo(RelationshipType from, RelationshipLabel type, RelationshipType to);
  
List<RelationshipEntity> findByFromAndFromRefInAndTypeAndTo(RelationshipType from, List<String> fromRef, RelationshipLabel type, RelationshipType to);
  
List<RelationshipEntity> findByFromAndTypeAndToAndToRefIn(RelationshipType from, RelationshipLabel type, RelationshipType to,
    List<String> toRef);

  List<RelationshipEntity> findByFromAndFromRefInAndToAndToRefIn(RelationshipType from, List<String> fromRef, RelationshipType to,
      List<String> toRef);

  List<RelationshipEntity> findByFromAndFromRefInAndTo(RelationshipType from, List<String> fromRef, RelationshipType to);

  List<RelationshipEntity> findByFromAndFromRefInAndTypeAndToAndToRefIn(RelationshipType from, List<String> fromRef, RelationshipLabel type, RelationshipType to,
      List<String> toRef);
  
  void deleteByToAndToRef(RelationshipType to,
      String toRef);
  
  void deleteByFromAndFromRef(RelationshipType from,
      String fromRef);
  
  @Update("{ '$set' : { 'fromRef' : ?2 } }")
  void findAndSetFromRefByFromAndFromRef(RelationshipType from,
      String fromRef, String newFromRef); 
  
  @Update("{ '$set' : { 'toRef' : ?2 } }")
  void findAndSetToRefByToAndToRef(RelationshipType to,
      String toRef, String newToRef); 
}
