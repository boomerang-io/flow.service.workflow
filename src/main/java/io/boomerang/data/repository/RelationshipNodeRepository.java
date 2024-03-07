package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.RelationshipNodeEntity;
import io.boomerang.model.enums.RelationshipNodeType;

public interface RelationshipNodeRepository extends MongoRepository<RelationshipNodeEntity, String> {
    
  Optional<RelationshipNodeEntity> findFirstByTypeAndRef(RelationshipNodeType type, String ref);
  
  List<RelationshipNodeEntity> findAllByTypeAndRefIn(RelationshipNodeType type, List<String> refs);
  
  Optional<RelationshipNodeEntity> findFirstByTypeAndSlug(RelationshipNodeType type, String slug);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, 'slug': ?1}}","{ '$set' : { 'slug' : ?2 } }"})
  RelationshipNodeEntity findAndSetSlugByTypeAndSlug(RelationshipNodeType type, String slug, 
      String newSlug);  
  
  void deleteByTypeAndSlug(RelationshipNodeType type,
      String slug); 
  
  void deleteByTypeAndRef(RelationshipNodeType type,
      String ref);
}
