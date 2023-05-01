package io.boomerang.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.security.model.TokenPermission;
import io.boomerang.v4.data.entity.TokenEntity;

public interface FlowTokenRepository extends MongoRepository<TokenEntity, String> {
  
  public List<TokenEntity> findByScope(TokenPermission scope);
  public List<TokenEntity> findByScopeAndTeamId(TokenPermission scope, String teamId);
  public TokenEntity findByToken(String token);
}
