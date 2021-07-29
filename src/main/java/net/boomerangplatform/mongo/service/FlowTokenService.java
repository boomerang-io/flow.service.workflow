package net.boomerangplatform.mongo.service;

import java.util.Date;
import java.util.List;
import net.boomerangplatform.mongo.entity.TokenEntity;

public interface FlowTokenService {
  
  
  public List<TokenEntity> findAllGlobalTokens();
  public List<TokenEntity> findAllTeamTokens(String teamId);
  public void deleteToken(String tokenId);
  public String createTeamToken(String teamId, Date expiryDate, String description);
  public String createSystemToken(Date expiryDate, String description);
  public TokenEntity validateToken(String value);

}
