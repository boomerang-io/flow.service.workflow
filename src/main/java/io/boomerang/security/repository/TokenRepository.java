package io.boomerang.security.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.security.entity.TokenEntity;

public interface TokenRepository extends MongoRepository<TokenEntity, String> {
  Optional<TokenEntity> findByToken(String token);
}
