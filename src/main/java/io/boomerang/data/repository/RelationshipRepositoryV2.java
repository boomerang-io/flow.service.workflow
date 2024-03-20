package io.boomerang.data.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import io.boomerang.data.entity.RelationshipConnectionEntity;
import io.boomerang.data.entity.RelationshipEntityV2;
import io.boomerang.data.entity.RelationshipEntityV2Graph;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;

public interface RelationshipRepositoryV2 extends MongoRepository<RelationshipEntityV2, String> {

  Integer countByTypeAndRefInAndConnectionsTo(RelationshipType type, List<String> ref, String to);
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}")
  Optional<RelationshipEntityV2> findFirstByTypeAndRefOrSlug(RelationshipType type, String ref);
  
  List<RelationshipEntityV2> findAllByTypeAndRefInAndConnectionsTo(RelationshipType type, List<String> ref, String to);
  
  List<RelationshipEntityV2> findAllByConnectionsLabelAndConnectionsTo(RelationshipLabel label, String to);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, 'slug': ?1}}","{ '$set' : { 'slug' : ?2 } }"})
  RelationshipEntityV2 findAndSetSlugByTypeAndSlug(RelationshipType type, String slug, 
      String newSlug);  
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}")
  void deleteByTypeAndRefOrSlug(RelationshipType type,
      String slug);
  
  @Query(value = "{ 'type': ?0, $or: [ { 'ref': ?1 }, { 'slug': ?1 } ] }", exists = true)
  boolean existsByTypeAndRefOrSlug(RelationshipType type,
      String ref);

  boolean existsByTypeAndSlugAndConnectionsTo(RelationshipType type, String slug, ObjectId to);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$_id', 'connectFromField':'id', 'connectToField': 'connections.to', 'as': 'children', restrictSearchWithMatch: {'connections.label': ?3 } } }"})
  RelationshipEntityV2Graph graphRelationshipsByLabelTo(RelationshipType type, String ref, String collection, RelationshipLabel label);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$_id', 'connectFromField':'id', 'connectToField': 'connections.to', 'as': 'children', restrictSearchWithMatch: {'type': ?3 } } }"})
  RelationshipEntityV2Graph graphRelationshipsByTypeTo(RelationshipType type, String ref, String collection, RelationshipType childType);
  
  @Aggregation(pipeline={"{'$match':{'type': 'USER', '$or': [{'slug': ?0},{'ref': ?0}]}}",
      "{ '$graphLookup' : { 'from' :  ?1, 'startWith': '$connections.to', 'connectFromField':'connections.to', 'connectToField': '_id', 'as': 'children' } }",
      "{ '$addFields' : { 'teams' : { '$map' : { 'input' : '$children', 'in' : { '$mergeObjects' : [ '$$this', { '$arrayElemAt' : [ '$connections', { '$indexOfArray': [ '$connections.to', '$$this._id' ] } ] } ] } } } } }"})
  RelationshipEntityV2Graph findUserTeamRelationships(String ref, String collection);
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}")
  @Update("{ '$push' : { 'connections' : ?2 } }")
  long pushConnectionByTypeAndRefOrSlug(RelationshipType type, String ref, RelationshipConnectionEntity connection);
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}], 'connections.to': ?2}")
  @Update("{ '$set' : { 'connections.$.data' : ?3 } }")
  long updateConnectionByTypeAndRefOrSlug(RelationshipType type, String ref, ObjectId to, Map<String, String> data);
}
