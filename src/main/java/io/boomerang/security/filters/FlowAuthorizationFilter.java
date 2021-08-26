package io.boomerang.security.filters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TokenEntity;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.mongo.service.FlowTokenService;
import io.boomerang.mongo.service.FlowUserService;
import io.boomerang.security.AuthorizationException;
import io.boomerang.security.model.FlowAuthenticationToken;
import io.boomerang.security.model.GlobalToken;
import io.boomerang.security.model.TeamToken;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.UserToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.impl.DefaultJwtParser;

public class FlowAuthorizationFilter extends BasicAuthenticationFilter {

  private static final String X_FORWARDED_USER = "x-forwarded-user";
  private static final String X_FORWARDED_EMAIL = "x-forwarded-email";
  
  private static final String X_ACCESS_TOKEN = "x-access-token";
  private static final String AUTHORIZATION_HEADER = "Authorization";

  @Value("${boomerang.authorization.basic.password:}")
  private String basicPassword;

  private static final Logger LOGGER = LogManager.getLogger();
  private FlowTokenService tokenService;
  private FlowUserService flowUserService;
  
  public FlowAuthorizationFilter(FlowTokenService tokenService, AuthenticationManager authManager,
      FlowUserService flowUserService, String basicPassword) {
    super(authManager);
    this.tokenService = tokenService;
    this.basicPassword = basicPassword;
    this.flowUserService = flowUserService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
      FilterChain chain) throws IOException, ServletException {

    LOGGER.info("Entering flow authorization filter");
    try {
      Authentication authentication = null;
      if (req.getHeader(AUTHORIZATION_HEADER) != null) {
        LOGGER.info("Detected Authorization header");
        authentication = getUserAuthentication(req);
      } else if (req.getHeader(X_FORWARDED_EMAIL) != null) { 
        LOGGER.info("Detected Github Authorization");
        authentication = getGithubUserAuthentication(req);
      }
      else if (req.getHeader(X_ACCESS_TOKEN) != null) {
        authentication = getTokenBasedAuthentication(req);
      }

      SecurityContextHolder.getContext().setAuthentication(authentication);
      chain.doFilter(req, res);
    } catch (final AuthorizationException e) {
      LOGGER.error(e);
    }
  }

  private Authentication getGithubUserAuthentication(HttpServletRequest req) {
    String email = req.getHeader(X_FORWARDED_EMAIL);
    String userName = req.getHeader(X_FORWARDED_USER);
    LOGGER.info("email: " + email);
    LOGGER.info("userName: " + userName);
    final UserToken userDetails = new UserToken(email, userName, "");
    if (email != null && !email.isBlank()) {
      final List<GrantedAuthority> authorities = new ArrayList<>();
      final UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(email, email, authorities);
      authToken.setDetails(userDetails);
      return authToken;
    }
    return null;
  }

  private Authentication getTokenBasedAuthentication(HttpServletRequest request) {
    String xAccessToken = request.getHeader(X_ACCESS_TOKEN);
    LOGGER.error("getTokenBasedAuthentication()");
    LOGGER.error(xAccessToken);
    TokenEntity token = tokenService.getAccessToken(xAccessToken);
    if (token != null) {
      final List<GrantedAuthority> authorities = new ArrayList<>();
      if (token.getScope() == TokenScope.global) {
        Token t = new GlobalToken();
        final FlowAuthenticationToken authToken = new FlowAuthenticationToken(authorities);
        authToken.setDetails(t);
        return authToken;
      } else if (token.getScope() == TokenScope.team) {
        Token t = new TeamToken();
        final FlowAuthenticationToken authToken = new FlowAuthenticationToken(authorities);
        authToken.setDetails(t);
        return authToken;
      } else if (token.getScope() == TokenScope.user) {
        String userId = token.getUserId();
        Optional<FlowUserEntity> user = flowUserService.getUserById(userId);
        if (user.isPresent()) {
          FlowUserEntity flowUser = user.get();
          String name = flowUser.getName();
          String email = flowUser.getEmail();
          final UserToken userDetails = new UserToken(userId, name, "");
          if (userId != null) {
            final UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(email, null, authorities);
            authToken.setDetails(userDetails);
            return authToken;
          }
        }
      }
    }
    return null;
  }

  private UsernamePasswordAuthenticationToken getUserAuthentication(HttpServletRequest request) // NOSONAR
  {
    final String token = request.getHeader(AUTHORIZATION_HEADER);
    
    LOGGER.info("Token Value");
    LOGGER.info(token);
    
    if (token.startsWith("Bearer ")) {
      final String jws = token.replace("Bearer ", "");
      Claims claims;
      String withoutSignature = jws.substring(0, jws.lastIndexOf('.') + 1);
      try {
        claims = (Claims) new DefaultJwtParser().parse(withoutSignature).getBody();
      } catch (ExpiredJwtException e) {
        claims = e.getClaims();

      }
      String userId = null;
      if (claims.get("emailAddress") != null) {
        userId = (String) claims.get("emailAddress");
      } else if (claims.get("email") != null) {
        userId = (String) claims.get("email");
      }

      String firstName = null;
      if (claims.get("firstName") != null) {
        firstName = (String) claims.get("firstName");
      } else if (claims.get("given_name") != null) {
        firstName = (String) claims.get("given_name");
      }

      String lastName = null;
      if (claims.get("lastName") != null) {
        lastName = (String) claims.get("lastName");
      } else if (claims.get("family_name") != null) {
        lastName = (String) claims.get("family_name");
      }

      firstName = santaize(firstName);
      lastName = santaize(lastName);

      final UserToken userDetails = new UserToken(userId, firstName, lastName);
      if (userId != null) {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        final UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);
        authToken.setDetails(userDetails);

        return authToken;

      }
      return null;
    } else if (token.startsWith("Basic ")) {

      
      
      String base64Credentials =
          request.getHeader(AUTHORIZATION_HEADER).substring("Basic".length()).trim();
      
      LOGGER.info("Base64 Details");
      LOGGER.info(base64Credentials);
      
      byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
      String credentials = new String(credDecoded, StandardCharsets.UTF_8);

      String password = "";
      final String[] values = credentials.split(":", 2);
      String username = values[0];
   
      if (values.length > 1) {
        password = values[1];
      }
      LOGGER.info(password);
      LOGGER.info(basicPassword);
      
      
      if (!basicPassword.equals(password)) {
        return null;
      }

      final UserToken userDetails = new UserToken(username, username, "");

      if (username != null) {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        final UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(username, password, authorities);
        authToken.setDetails(userDetails);
        return authToken;
      }
      return null;
    }
    return null;
  }

  private String santaize(String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }
    String cleanString = value;
    try {
      cleanString = java.net.URLDecoder.decode(value, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      return value;
    }
    cleanString = cleanString.toLowerCase();
    cleanString = WordUtils.capitalizeFully(cleanString);
    return cleanString;
  }

}
