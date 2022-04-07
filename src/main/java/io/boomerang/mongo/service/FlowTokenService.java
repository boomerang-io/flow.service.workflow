package io.boomerang.mongo.service;

import java.util.Date;
import java.util.List;
import io.boomerang.model.Token;
import io.boomerang.model.TokenResponse;
import io.boomerang.mongo.entity.TokenEntity;

public interface FlowTokenService {
  
  public List<Token> findAllGlobalTokens();
  public List<Token> findAllTeamTokens(String teamId);
  public void deleteToken(String tokenId);
  public TokenResponse createTeamToken(String teamId, Date expiryDate, String description);
  public TokenResponse createSystemToken(Date expiryDate, String description);
  public TokenEntity validateToken(String value);
  public TokenEntity getAccessToken(String token);
}
