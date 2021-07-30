package net.boomerangplatform.mongo.service;

import java.util.Date;
import java.util.List;
import net.boomerangplatform.model.Token;
import net.boomerangplatform.model.TokenResponse;
import net.boomerangplatform.mongo.entity.TokenEntity;

public interface FlowTokenService {
  
  public List<Token> findAllGlobalTokens();
  public List<Token> findAllTeamTokens(String teamId);
  public void deleteToken(String tokenId);
  public TokenResponse createTeamToken(String teamId, Date expiryDate, String description);
  public TokenResponse createSystemToken(Date expiryDate, String description);
  public TokenEntity validateToken(String value);

}
