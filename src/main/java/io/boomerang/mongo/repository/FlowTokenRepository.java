package io.boomerang.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.mongo.entity.TokenEntity;
import io.boomerang.mongo.model.TokenScope;

public interface FlowTokenRepository extends MongoRepository<TokenEntity, String> {
  
  public List<TokenEntity> findByScope(TokenScope scope);
  public List<TokenEntity> findByScopeAndTeamId(TokenScope scope, String teamId);
  public TokenEntity findByToken(String token);
}
