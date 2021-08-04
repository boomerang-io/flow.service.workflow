package net.boomerangplatform.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.TokenEntity;
import net.boomerangplatform.mongo.model.TokenScope;

public interface FlowTokenRepository extends MongoRepository<TokenEntity, String> {
  
  public List<TokenEntity> findByScope(TokenScope scope);
  public List<TokenEntity> findByScopeAndTeamId(TokenScope scope, String teamId);
  public TokenEntity findByToken(String token);
}
