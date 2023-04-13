package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.v4.data.entity.RelationshipEntity;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;

public interface RelationshipService {

  RelationshipEntity addRelationshipRefForCurrentScope(RelationshipRef fromType, String fromRef);

  RelationshipEntity addRelationshipRef(RelationshipRef fromType, String fromRef,
      RelationshipRef toType, Optional<String> toRef);

  RelationshipEntity addRelationshipRef(RelationshipRef fromType, String fromRef,
      RelationshipType relationship, RelationshipRef toType, Optional<String> toRef);

  void removeRelationships(RelationshipRef fromType, List<String> fromRefs, RelationshipRef toType,
      List<String> toRefs);

  Optional<RelationshipEntity> getRelationship(RelationshipRef fromType, String fromRef,
      RelationshipType relationship);

  List<String> getFilteredRefs(Optional<RelationshipRef> from, Optional<List<String>> fromRefs,
      Optional<RelationshipType> type, Optional<RelationshipRef> to, Optional<List<String>> toRefs);
}
