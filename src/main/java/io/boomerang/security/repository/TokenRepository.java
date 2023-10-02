package io.boomerang.security.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.security.entity.TokenEntity;
import io.boomerang.security.model.AuthType;

public interface TokenRepository extends MongoRepository<TokenEntity, String> {
  Optional<TokenEntity> findByToken(String token);
  
  Optional<List<TokenEntity>> findByPrincipalAndType(String principal, AuthType type);
  
  void deleteAllByPrincipal(String principal);
}
