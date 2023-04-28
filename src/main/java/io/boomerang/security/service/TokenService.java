package io.boomerang.security.service;

import javax.validation.Valid;
import org.springframework.data.domain.Page;
import io.boomerang.model.CreateTokenRequest;
import io.boomerang.model.CreateTokenResponse;
import io.boomerang.model.TokenResponse;
import io.boomerang.v4.data.entity.UserEntity;

public interface TokenService {
  public TokenResponse getToken(String value);
  public CreateTokenResponse createUserSessionToken(UserEntity user);
  public CreateTokenResponse createToken(CreateTokenRequest token);
  public boolean validateToken(String token);
  public boolean deleteToken(@Valid String id);
  public Page<TokenResponse> getAllTokens();
}
