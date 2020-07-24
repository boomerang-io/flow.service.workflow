package net.boomerangplatform.mongo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.FlowUserEntity;

public interface FlowUserRepository extends MongoRepository<FlowUserEntity, String> {

  Long countByEmailIgnoreCase(String email);

  FlowUserEntity findByEmailIgnoreCase(String email);

  Page<FlowUserEntity> findByNameLikeIgnoreCaseOrEmailLikeIgnoreCase(String term, String term2,
      Pageable pageable);

  List<FlowUserEntity> findByFlowTeamsContaining(List<String> teamIds);

}
