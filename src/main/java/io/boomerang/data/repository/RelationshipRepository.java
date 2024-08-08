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
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.entity.RelationshipEntityGraph;
import io.boomerang.model.enums.RelationshipLabel;
import io.boomerang.model.enums.RelationshipType;

public interface RelationshipRepository extends MongoRepository<RelationshipEntity, String> {

  Integer countByTypeAndRefInAndConnectionsTo(RelationshipType type, List<String> ref, String to);
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}")
  Optional<RelationshipEntity> findFirstByTypeAndRefOrSlug(RelationshipType type, String ref);
  
  List<RelationshipEntity> findAllByTypeAndRefInAndConnectionsTo(RelationshipType type, List<String> ref, String to);
  
  List<RelationshipEntity> findAllByConnectionsLabelAndConnectionsTo(RelationshipLabel label, String to);
  
  @Query("{'type': ?0, 'slug': ?1}")
  @Update("{ '$set' : { 'slug' : ?2 } }")
  long findAndSetSlugByTypeAndSlug(RelationshipType type, String slug, 
      String newSlug);  
  
  @Query(value = "{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}", delete = true)
  void deleteByTypeAndRefOrSlug(RelationshipType type,
      String slug);
  
  @Query(value = "{ 'type': ?0, $or: [ { 'ref': ?1 }, { 'slug': ?1 } ] }", exists = true)
  boolean existsByTypeAndRefOrSlug(RelationshipType type,
      String ref);

  boolean existsByTypeAndSlugAndConnectionsTo(RelationshipType type, String slug, ObjectId to);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$_id', 'connectFromField':'id', 'connectToField': 'connections.to', 'as': 'children', restrictSearchWithMatch: {'type': ?3, '$or' : [{ 'slug' : { '$in': ?4 } }, { 'ref' : { '$in': ?4 } } ] } } }"})
  RelationshipEntityGraph graphRelationshipsByTypeToAndIn(RelationshipType type, String ref, String collection, RelationshipType childType, List<String> childRefs);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$_id', 'connectFromField':'id', 'connectToField': 'connections.to', 'as': 'children', restrictSearchWithMatch: {'type': ?3 } } }"})
  RelationshipEntityGraph graphRelationshipsByTypeTo(RelationshipType type, String ref, String collection, RelationshipType childType);
  
  @Aggregation(pipeline={"{'$match':{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}}",
      "{ '$graphLookup' : { 'from' :  ?2, 'startWith': '$connections.to', 'connectFromField':'connections.to', 'connectToField': '_id', 'as': 'children', restrictSearchWithMatch: {'type': ?3 } } }"})
  RelationshipEntityGraph graphRelationshipsByTypeFrom(RelationshipType type, String ref, String collection, RelationshipType childType);
  
  @Aggregation(pipeline={"{'$match':{'type': 'USER', '$or': [{'slug': ?0},{'ref': ?0}]}}",
      "{ '$graphLookup' : { 'from' :  ?1, 'startWith': '$connections.to', 'connectFromField':'connections.to', 'connectToField': '_id', 'as': 'children' } }",
      "{ '$addFields' : { 'teams' : { '$map' : { 'input' : '$children', 'in' : { '$mergeObjects' : [ '$$this', { '$arrayElemAt' : [ '$connections', { '$indexOfArray': [ '$connections.to', '$$this._id' ] } ] } ] } } } } }"})
  RelationshipEntityGraph findUserTeamRelationships(String ref, String collection);
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}]}")
  @Update("{ '$push' : { 'connections' : ?2 } }")
  long pushConnectionByTypeAndRefOrSlug(RelationshipType type, String ref, RelationshipConnectionEntity connection);
  
  @Query("{'type': ?0, '$or': [{'slug': ?1},{'ref': ?1}], 'connections.to': ?2}")
  @Update("{ '$set' : { 'connections.$.data' : ?3 } }")
  long updateConnectionByTypeAndRefOrSlug(RelationshipType type, String ref, ObjectId to, Map<String, String> data);
  
  @Query("{}")
  @Update("{ '$pull' : { 'connections' : { 'to' : ?0 } } }")
  long removeAllConnectionsByTo(ObjectId ref);
  
  @Query("{'type': ?0, '$or': [{ 'slug' : { '$in': ?1 } }, { 'ref' : { '$in': ?1 } } ]}")
  @Update("{ '$pull' : { 'connections' : { 'to' : ?2 } } }")
  long removeConnectionByTo(RelationshipType type, List<String> ref, ObjectId to);
}
