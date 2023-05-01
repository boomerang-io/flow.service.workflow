package io.boomerang.v4.data.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.v4.data.entity.TokenEntity;

public interface TokenRepository extends MongoRepository<TokenEntity, String> {
  Optional<TokenEntity> findByToken(String token);
}
