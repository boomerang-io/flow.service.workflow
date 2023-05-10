package io.boomerang.security.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.boomerang.security.model.CreateTokenRequest;
import io.boomerang.security.model.CreateTokenResponse;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenType;

public interface TokenService {
  public Token get(String value);
  public Token createUserSessionToken(String email, String firstName, String lastName, boolean isProfile);
  public CreateTokenResponse create(CreateTokenRequest token);
  public boolean validate(String token);
  public boolean delete(@Valid String id);
  public Page<Token> query(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<TokenType>> queryTypes);
}
