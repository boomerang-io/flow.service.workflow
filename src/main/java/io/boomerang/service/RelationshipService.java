package io.boomerang.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.model.enums.RelationshipNodeType;
import io.boomerang.model.enums.RelationshipLabel;

public interface RelationshipService {

  RelationshipEntity addRelationshipRef(RelationshipNodeType fromType, String fromRef,
      RelationshipLabel relationship, RelationshipNodeType toType, Optional<String> toRef,
      Optional<Map<String, Object>> data);

  RelationshipEntity patchRelationshipData(RelationshipNodeType fromType, String fromRef,
      RelationshipLabel relationship, Map<String, Object> data);

  void removeRelationships(RelationshipNodeType fromType, List<String> fromRefs, RelationshipNodeType toType,
      List<String> toRefs);

  void removeRelationships(RelationshipNodeType fromType, List<String> fromRefs, RelationshipNodeType toType);

  void removeRelationships(RelationshipNodeType toType, String toRef);

  void removeUserTeamRelationship(String toRef);

  Optional<RelationshipEntity> getRelationship(RelationshipNodeType fromType, String fromRef,
      RelationshipLabel relationship);

  List<String> getFilteredFromRefs(Optional<RelationshipNodeType> from, Optional<List<String>> fromRefs,
      Optional<RelationshipLabel> type, Optional<RelationshipNodeType> to, Optional<List<String>> toRefs);

  List<String> getFilteredToRefs(Optional<RelationshipNodeType> from, Optional<List<String>> fromRefs,
      Optional<RelationshipLabel> type, Optional<RelationshipNodeType> to, Optional<List<String>> toRefs);

  Optional<String> getRelationshipRef(RelationshipNodeType fromType, String fromRef,
      RelationshipLabel relationship);

  Map<String, String> getMyTeamRefsAndRoles(String userId);

  List<RelationshipEntity> getFilteredRels(Optional<RelationshipNodeType> from,
      Optional<List<String>> fromRefs, Optional<RelationshipLabel> type,
      Optional<RelationshipNodeType> to, Optional<List<String>> toRefs, boolean elevate);

  void removeRelationshipById(String id);

  void updateTeamNodeSlug(String oldRef, String newRef);
}
