package io.boomerang.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.RelationshipLabel;

public interface RelationshipService {

  RelationshipEntity addRelationshipRef(RelationshipType fromType, String fromRef,
      RelationshipLabel relationship, RelationshipType toType, Optional<String> toRef,
      Optional<Map<String, Object>> data);

  RelationshipEntity patchRelationshipData(RelationshipType fromType, String fromRef,
      RelationshipLabel relationship, Map<String, Object> data);

  void removeRelationships(RelationshipType fromType, List<String> fromRefs, RelationshipType toType,
      List<String> toRefs);

  void removeRelationships(RelationshipType fromType, List<String> fromRefs, RelationshipType toType);

  void removeRelationships(RelationshipType toType, String toRef);

  void removeUserTeamRelationship(String toRef);

  Optional<RelationshipEntity> getRelationship(RelationshipType fromType, String fromRef,
      RelationshipLabel relationship);

  List<String> getFilteredFromRefs(Optional<RelationshipType> from, Optional<List<String>> fromRefs,
      Optional<RelationshipLabel> type, Optional<RelationshipType> to, Optional<List<String>> toRefs);

  List<String> getFilteredToRefs(Optional<RelationshipType> from, Optional<List<String>> fromRefs,
      Optional<RelationshipLabel> type, Optional<RelationshipType> to, Optional<List<String>> toRefs);

  Optional<String> getRelationshipRef(RelationshipType fromType, String fromRef,
      RelationshipLabel relationship);

  Map<String, String> getMyTeamRefsAndRoles(String userId);

  List<RelationshipEntity> getFilteredRels(Optional<RelationshipType> from,
      Optional<List<String>> fromRefs, Optional<RelationshipLabel> type,
      Optional<RelationshipType> to, Optional<List<String>> toRefs, boolean elevate);

  void removeRelationshipById(String id);

  void updateNodeSlug(RelationshipType type, String oldRef, String newRef);
}
