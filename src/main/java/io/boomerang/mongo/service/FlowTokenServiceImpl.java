package io.boomerang.mongo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.model.Token;
import io.boomerang.model.TokenResponse;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TokenEntity;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.mongo.repository.FlowTokenRepository;
import io.boomerang.service.UserIdentityService;

@Service
public class FlowTokenServiceImpl implements FlowTokenService {

  @Autowired
  private FlowTokenRepository tokenRepository;

  @Autowired
  private UserIdentityService userService;
  
  @Override
  public List<Token> findAllGlobalTokens() {
    List<TokenEntity> tokens = tokenRepository.findByScope(TokenScope.global);
    return buildTokenList(tokens);
  }
  
  @Override
  public List<Token> findAllTeamTokens(String teamId) {
    List<TokenEntity> tokens = tokenRepository.findByScopeAndTeamId(TokenScope.team, teamId);
    return buildTokenList(tokens);
  }

  private List<Token> buildTokenList(List<TokenEntity> tokens) {
    List<Token> tokenList = new LinkedList<>();
    for (TokenEntity te : tokens) {
      String creatorId = te.getCreatorId();
      Token token = convertEntityToToken(te, creatorId);
      tokenList.add(token);
    }
    return tokenList;
  }

  private Token convertEntityToToken(TokenEntity te, String creatorId) {
    Token token = new Token();
    token.setId(te.getId());
    token.setDescription(te.getDescription());
    token.setExpiryDate(te.getExpiryDate());
    token.setCreationDate(te.getCreationDate());
    token.setCreatorId(creatorId);
    FlowUserEntity user = userService.getUserByID(creatorId); 
    if (user != null) {
      token.setCreatorName(user.getName());
    }
    Date currentDate = new Date();
    if (token.getExpiryDate() != null && token.getExpiryDate().getTime () < currentDate.getTime()) {
       token.setExpired(true);
    }
    return token;
  }

  @Override
  public void deleteToken(String tokenId) {
    this.tokenRepository.deleteById(tokenId);
  }

  @Override
  public TokenResponse createTeamToken(String teamId, Date expiryDate, String description) {
    FlowUserEntity currentUser = userService.getCurrentUser();
    String creatorId = currentUser.getId();
    TokenEntity tokenEntity = createNewToken(expiryDate, description, TokenScope.team, creatorId);
    tokenEntity.setTeamId(teamId);
    
    String prefix = getPrefixForScope(TokenScope.team);
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();
    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
  
    this.tokenRepository.save(tokenEntity);
    Token token = convertEntityToToken(tokenEntity, creatorId);
    TokenResponse response = new TokenResponse();
    BeanUtils.copyProperties(token, response);
    response.setTokenValue(uniqueToken);
    return response;
  }

  @Override
  public TokenResponse createSystemToken(Date expiryDate, String description) {
    FlowUserEntity currentUser = userService.getCurrentUser();
    String creatorId = currentUser.getId();
    TokenEntity tokenEntity = createNewToken(expiryDate, description, TokenScope.global, creatorId);
    String prefix = getPrefixForScope(TokenScope.global);
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString();
    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
    
    this.tokenRepository.save(tokenEntity);
    Token token = convertEntityToToken(tokenEntity, creatorId);
    TokenResponse response = new TokenResponse();
    response.setTokenValue(uniqueToken);
    BeanUtils.copyProperties(token, response);
    
    return response;
  }

  private TokenEntity createNewToken(Date expiryDate, String description, TokenScope scope, String creatorId) {
    TokenEntity tokenEntity = new TokenEntity();
    tokenEntity.setCreationDate(new Date());
    tokenEntity.setExpiryDate(expiryDate);
    tokenEntity.setDescription(description);
    tokenEntity.setScope(scope);
    tokenEntity.setCreatorId(creatorId);
    
    return tokenEntity;
  }

  private String getPrefixForScope(TokenScope scope) {
    if (TokenScope.team == scope) {
      return "bft";
    } else if (TokenScope.global == scope) {
      return "bfg";
    } else if (TokenScope.user == scope) {
      return "bfp";
    }
    return null;
  }

  @Override
  public TokenEntity validateToken(String value) {
    return null;
  }

  public String hashString(String originalString) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < hash.length; i++) {
        String hex = Integer.toHexString(0xff & hash[i]);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  @Override
  public TokenEntity getAccessToken(String token) {
    final String hashToken = hashString(token);
    return tokenRepository.findByToken(hashToken);
  }
}

