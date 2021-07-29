package net.boomerangplatform.mongo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.TokenEntity;
import net.boomerangplatform.mongo.model.TokenScope;
import net.boomerangplatform.mongo.repository.FlowTokenRepository;

@Service
public class FlowTokenServiceImpl implements FlowTokenService {

  @Autowired
  private FlowTokenRepository tokenRepository;

  @Override
  public List<TokenEntity> findAllGlobalTokens() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<TokenEntity> findAllTeamTokens(String teamId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void deleteToken(String tokenId) {
    // TODO Auto-generated method stub

  }

  @Override
  public String createTeamToken(String teamId, Date expiryDate, String description) {

    TokenEntity tokenEntity = createNewToken(expiryDate, description, TokenScope.team);
    tokenEntity.setScopeId(teamId);
    String prefix = getPrefixForScope(TokenScope.team);
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();
    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
    return uniqueToken;
  }

  @Override
  public String createSystemToken(Date expiryDate, String description) {
    TokenEntity tokenEntity = createNewToken(expiryDate, description, TokenScope.global);
    String prefix = getPrefixForScope(TokenScope.global);
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString();
    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
    return uniqueToken;
  }

  private TokenEntity createNewToken(Date expiryDate, String description, TokenScope scope) {
    TokenEntity tokenEntity = new TokenEntity();
    tokenEntity.setCreationDate(new Date());
    tokenEntity.setExpiryDate(expiryDate);
    tokenEntity.setDescription(description);
    tokenEntity.setScope(scope);
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
}

