package io.boomerang.security.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.devabl.userprofiles.entity.TokenEntity;
import com.devabl.userprofiles.entity.UserEntity;
import com.devabl.userprofiles.model.CreateTokenRequest;
import com.devabl.userprofiles.model.CreateTokenResponse;
import com.devabl.userprofiles.model.ListTokenResponse;
import com.devabl.userprofiles.model.TokenResponse;
import com.devabl.userprofiles.model.TokenType;
import com.devabl.userprofiles.repository.TokenRepository;
import com.devabl.userprofiles.security.model.TokenTypePrefix;

@Service
public class TokenServiceImpl implements TokenService {

  @Autowired
  private TokenRepository tokenRepository;

  @Value("${userprofiles.tokens.user.duration}")
  private Integer MAX_USER_SESSION_TOKEN_DURATION;

  /**
   *  TODO expand support for users and system tokens
   */
  @Override
  public CreateTokenResponse createToken(CreateTokenRequest token) {

    TokenEntity newToken = new TokenEntity();
    newToken.setCreationDate(new Date());
    newToken.setType(token.getType());
    newToken.setExpirationDate(token.getExpiryDate());
    newToken.setValid(true);
    newToken.setScopes(token.getScopes());

    BeanUtils.copyProperties(token, newToken);
    String prefix = TokenTypePrefix.system.label;
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();

    final String hashToken = hashString(uniqueToken);
    newToken.setToken(hashToken);

    tokenRepository.save(newToken);
    CreateTokenResponse response = new CreateTokenResponse();
    response.setValue(uniqueToken);
    response.setId(newToken.getId());

    return response;
  }

  public String hashString(String originalString) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte element : hash) {
        String hex = Integer.toHexString(0xff & element);
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
  public boolean validateToken(String token) {
    String hash = hashString(token);
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findByToken(hash);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      Date currentDate = new Date();
      boolean validToken = tokenEntity.isValid();

      if (validToken && tokenEntity.getExpirationDate().after(currentDate)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean deleteToken(@Valid String id) {
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findById(id);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      tokenEntity.setValid(false);
      return true;
    }
    return false;
  }

  @Override
  public ListTokenResponse getAllTokens() {
    /** @TODO: complete token listing. **/
    ListTokenResponse response = new ListTokenResponse();
    return response;
  }

  @Override
  public TokenResponse getToken(String token) {
    String hash = hashString(token);
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findByToken(hash);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      Date currentDate = new Date();
      boolean validToken = tokenEntity.isValid();

      if (validToken && tokenEntity.getExpirationDate().after(currentDate)) {
        TokenResponse response = new TokenResponse();
        BeanUtils.copyProperties(tokenEntity, response);
        return response;
      }
    }
    return null;
  }

  @Override
  public CreateTokenResponse createUserSessionToken(UserEntity user) {

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR, MAX_USER_SESSION_TOKEN_DURATION);
    Date expiryDate = cal.getTime();


    TokenEntity newToken = new TokenEntity();
    newToken.setCreationDate(new Date());
    newToken.setType(TokenType.user);
    newToken.setExpirationDate(expiryDate);
    newToken.setValid(true);
    newToken.setCreatedBy(user);

    String prefix = TokenTypePrefix.user.label;
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();

    final String hashToken = hashString(uniqueToken);
    newToken.setToken(hashToken);
    tokenRepository.save(newToken);

    CreateTokenResponse response = new CreateTokenResponse();
    response.setValue(uniqueToken);
    response.setId(newToken.getId());
    response.setExpiryDate(expiryDate);

    return response;
  }
}
