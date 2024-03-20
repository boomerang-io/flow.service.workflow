package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import io.boomerang.data.entity.RelationshipNodeEntity;
import io.boomerang.data.entity.RelationshipNodeEntityAggregate;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;

public interface RelationshipNodeRepository extends MongoRepository<RelationshipNodeEntity, String> {
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}")
  Optional<RelationshipNodeEntity> findFirstByTypeAndRefOrSlug(RelationshipType type, String ref);
  
  List<RelationshipNodeEntity> findAllByTypeAndRefIn(RelationshipType type, List<String> refs);
  
  Optional<RelationshipNodeEntity> findFirstByTypeAndSlug(RelationshipType type, String slug);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, 'slug': ?1}}","{ '$set' : { 'slug' : ?2 } }"})
  RelationshipNodeEntity findAndSetSlugByTypeAndSlug(RelationshipType type, String slug, 
      String newSlug);  
  
  void deleteByTypeAndRefOrSlug(RelationshipType type,
      String slug); 
  
  boolean existsByTypeAndSlug(RelationshipType type,
      String slug);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$_id', 'connectFromField':'id', 'connectToField': 'to', 'as': 'paths', restrictSearchWithMatch: {'label': ?3 } } }",
      "{ '$graphLookup' : { 'from' :  ?4, 'startWith': '$paths.from', 'connectFromField':'paths.from', 'connectToField': '_id', 'as': 'children' } }"})
  RelationshipNodeEntityAggregate findRelationshipsByGraphTo(RelationshipType type, String ref, String edgeCollection, RelationshipLabel label, String nodeCollection);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$_id', 'connectFromField':'id', 'connectToField': 'from', 'as': 'paths', restrictSearchWithMatch: {'label': ?3 } } }",
      "{ '$graphLookup' : { 'from' :  ?4, 'startWith': '$paths.to', 'connectFromField':'paths.to', 'connectToField': '_id', 'as': 'children' } }"})
  RelationshipNodeEntityAggregate findRelationshipsByGraphFrom(RelationshipType type, String ref, String edgeCollection, RelationshipLabel label, String nodeCollection);
}
