package io.boomerang.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;

public interface RelationshipService {

  RelationshipEntity addRelationshipRefForCurrentScope(RelationshipRef fromType, String fromRef);

  RelationshipEntity addRelationshipRef(RelationshipRef fromType, String fromRef,
      RelationshipType relationship, RelationshipRef toType, Optional<String> toRef,
      Optional<Map<String, Object>> data);

  RelationshipEntity patchRelationshipData(RelationshipRef fromType, String fromRef,
      RelationshipType relationship, Map<String, Object> data);

  void removeRelationships(RelationshipRef fromType, List<String> fromRefs, RelationshipRef toType,
      List<String> toRefs);

  void removeUserTeamRelationship(String toRef);

  Optional<RelationshipEntity> getRelationship(RelationshipRef fromType, String fromRef,
      RelationshipType relationship);

  List<String> getFilteredFromRefs(Optional<RelationshipRef> from, Optional<List<String>> fromRefs,
      Optional<RelationshipType> type, Optional<RelationshipRef> to, Optional<List<String>> toRefs);

  List<String> getFilteredToRefs(Optional<RelationshipRef> from, Optional<List<String>> fromRefs,
      Optional<RelationshipType> type, Optional<RelationshipRef> to, Optional<List<String>> toRefs);

  Optional<String> getRelationshipRef(RelationshipRef fromType, String fromRef,
      RelationshipType relationship);

  Map<String, String> getMyTeamRefsAndRoles(String userId);

  List<RelationshipEntity> getFilteredRels(Optional<RelationshipRef> from,
      Optional<List<String>> fromRefs, Optional<RelationshipType> type,
      Optional<RelationshipRef> to, Optional<List<String>> toRefs, boolean elevate);

  void removeRelationshipById(String id);
}
