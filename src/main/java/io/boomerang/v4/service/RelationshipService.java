package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.v4.model.enums.RelationshipRefType;

public interface RelationshipService {

  List<String> getFilteredRefs(RelationshipRefType type, Optional<List<String>> typeIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes);

  List<String> getFilteredRefsForUserEmail(RelationshipRefType type,
      Optional<List<String>> typeRefs, Optional<List<String>> teamIds,
      Optional<List<String>> scopes, String userEmail);

  void createRelationshipRef(String fromType, String fromRef);

  boolean doesRelationshipExist(RelationshipRefType type, String typeRef);
}
