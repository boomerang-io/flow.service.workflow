package io.boomerang.v4.data.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.UserEntity;

public interface UserRepository extends MongoRepository<UserEntity, String> {

  Long countByEmailIgnoreCase(String email);

  UserEntity findByEmailIgnoreCase(String email);

  Page<UserEntity> findByNameLikeIgnoreCaseOrEmailLikeIgnoreCase(String term, String term2,
      Pageable pageable);

//  List<UserEntity> findByFlowTeamsContaining(List<String> teamIds);

}
