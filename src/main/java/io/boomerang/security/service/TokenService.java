package io.boomerang.security.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.security.model.CreateTokenRequest;
import io.boomerang.security.model.CreateTokenResponse;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.AuthType;

public interface TokenService {
  public Token get(String value);
  public Token createUserSessionToken(String email, String firstName, String lastName, boolean allowActivation, boolean allowUserCreation);
  public CreateTokenResponse create(CreateTokenRequest token);
  public boolean validate(String token);
  public boolean delete(@Valid String id);
  Page<Token> query(Optional<Date> from, Optional<Date> to, Optional<Integer> queryLimit,
      Optional<Integer> queryPage, Optional<Direction> queryOrder, Optional<String> querySort,
      Optional<List<AuthType>> queryTypes, Optional<List<String>> queryPrincipals);
}
