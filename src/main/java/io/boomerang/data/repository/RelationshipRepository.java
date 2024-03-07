package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Update;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipNodeType;

public interface RelationshipRepository extends MongoRepository<RelationshipEntity, String> {
//
  Optional<RelationshipEntity> findByFromAndFromRefAndType(RelationshipNodeType from, String fromRef, RelationshipLabel type);
//  
  List<RelationshipEntity> findByFromAndTypeAndTo(RelationshipNodeType from, RelationshipLabel type, RelationshipNodeType to);
  
List<RelationshipEntity> findByFromAndFromRefInAndTypeAndTo(RelationshipNodeType from, List<String> fromRef, RelationshipLabel type, RelationshipNodeType to);
  
List<RelationshipEntity> findByFromAndTypeAndToAndToRefIn(RelationshipNodeType from, RelationshipLabel type, RelationshipNodeType to,
    List<String> toRef);

  List<RelationshipEntity> findByFromAndFromRefInAndToAndToRefIn(RelationshipNodeType from, List<String> fromRef, RelationshipNodeType to,
      List<String> toRef);

  List<RelationshipEntity> findByFromAndFromRefInAndTo(RelationshipNodeType from, List<String> fromRef, RelationshipNodeType to);

  List<RelationshipEntity> findByFromAndFromRefInAndTypeAndToAndToRefIn(RelationshipNodeType from, List<String> fromRef, RelationshipLabel type, RelationshipNodeType to,
      List<String> toRef);
  
  void deleteByToAndToRef(RelationshipNodeType to,
      String toRef);
  
  void deleteByFromAndFromRef(RelationshipNodeType from,
      String fromRef);
  
  @Update("{ '$set' : { 'fromRef' : ?2 } }")
  void findAndSetFromRefByFromAndFromRef(RelationshipNodeType from,
      String fromRef, String newFromRef); 
  
  @Update("{ '$set' : { 'toRef' : ?2 } }")
  void findAndSetToRefByToAndToRef(RelationshipNodeType to,
      String toRef, String newToRef); 
}
