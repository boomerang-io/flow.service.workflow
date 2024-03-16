package io.boomerang.data.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.data.entity.RelationshipEdgeEntity;
import io.boomerang.model.enums.RelationshipLabel;

public interface RelationshipEdgeRepository extends MongoRepository<RelationshipEdgeEntity, String> {
  
  Optional<RelationshipEdgeEntity> findFirstByFromAndLabelAndTo(String from, RelationshipLabel label, String to);
  
  List<RelationshipEdgeEntity> findAllByLabelAndTo(RelationshipLabel label, String to);
  
  Integer countByFromInAndTo(List<String> from, String to);
  
  Integer countByFromAndLabelAndTo(String from, RelationshipLabel label, String to);  
  
}
