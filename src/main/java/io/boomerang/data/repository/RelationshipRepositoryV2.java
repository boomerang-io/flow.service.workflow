package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import io.boomerang.data.entity.RelationshipConnectionEntity;
import io.boomerang.data.entity.RelationshipEntityV2;
import io.boomerang.data.entity.RelationshipEntityV2Aggregate;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipNodeType;

public interface RelationshipRepositoryV2 extends MongoRepository<RelationshipEntityV2, String> {

  Integer countByTypeAndRefInAndConnectionsTo(RelationshipNodeType type, List<String> ref, String to);
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}")
  Optional<RelationshipEntityV2> findFirstByTypeAndRefOrSlug(RelationshipNodeType type, String ref);
  
  List<RelationshipEntityV2> findAllByTypeAndRefInAndConnectionsTo(RelationshipNodeType type, List<String> ref, String to);
  
  List<RelationshipEntityV2> findAllByConnectionsLabelAndConnectionsTo(RelationshipLabel label, String to);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, 'slug': ?1}}","{ '$set' : { 'slug' : ?2 } }"})
  RelationshipEntityV2 findAndSetSlugByTypeAndSlug(RelationshipNodeType type, String slug, 
      String newSlug);  
  
  void deleteByTypeAndRefOrSlug(RelationshipNodeType type,
      String slug);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$_id', 'connectFromField':'id', 'connectToField': 'connections.to', 'as': 'children', restrictSearchWithMatch: {'connections.label': ?3 } } }"})
  RelationshipEntityV2Aggregate findRelationshipsByLabel(RelationshipNodeType type, String ref, String collection, RelationshipLabel label);
  
  @Update("{ '$push' : { 'connections' : ?2 } }")
  long findAndPushConnectionByTypeAndRefOrSlug(RelationshipNodeType type, String ref, RelationshipConnectionEntity connection);
  
  @Update("{ '$set' : { 'connections.$.data.role' : ?4 } }")
  long findAndUpdateConnectionByTypeAndRefOrSlugAndConnectionsLabelAndConnectionsTo(RelationshipNodeType type, String ref, RelationshipLabel label, String to, String role);
}
