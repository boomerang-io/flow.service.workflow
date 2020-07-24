package net.boomerangplatform.security.filters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.impl.DefaultJwtParser;
import net.boomerangplatform.security.AuthorizationException;
import net.boomerangplatform.security.model.UserDetails;
import net.boomerangplatform.security.service.ApiTokenService;

public class BoomerangAuthorizationFilter extends BasicAuthenticationFilter {

  private final boolean checkJwt;
  private final String jwtSecret;

  private final ApiTokenService tokenService;

  @Value("${core.authorization.basic.password:}")
  private String basicPassword;
  
  private static final String WEBHEADER = "X-WEBAUTH-EMAIL";
  

  public BoomerangAuthorizationFilter(ApiTokenService tokenService,
      AuthenticationManager authManager, String jwtSecret, boolean checkJwt, String basicPassword) {
    super(authManager);
    this.jwtSecret = jwtSecret;
    this.checkJwt = checkJwt;
    this.tokenService = tokenService;
    this.basicPassword = basicPassword;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
      FilterChain chain) throws IOException, ServletException {
    try {
      final UsernamePasswordAuthenticationToken authentication = getAuthentication(req);
      SecurityContextHolder.getContext().setAuthentication(authentication);
      chain.doFilter(req, res);
    } catch (final AuthorizationException e) {
      e.printStackTrace();
    }
  }

  private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) // NOSONAR
  {

    if (request.getHeader("Authorization") != null) {

      final String token = request.getHeader("Authorization");

      if (token.startsWith("Bearer ")) {

        final String jws = token.replace("Bearer ", "");
        tokenService.storeUserToken(jws);
        Claims claims;
        if (checkJwt) {
          Jws<Claims> info = Jwts.parser().setSigningKey(jwtSecret.getBytes()).parseClaimsJws(jws);
          claims = info.getBody();
        } else {
          String withoutSignature = jws.substring(0, jws.lastIndexOf('.') + 1);

          try {
            claims = (Claims) new DefaultJwtParser().parse(withoutSignature).getBody();
          } catch (ExpiredJwtException e) {
            claims = e.getClaims();
          }
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

        final UserDetails userDetails = new UserDetails(userId, firstName, lastName);

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
            request.getHeader("Authorization").substring("Basic".length()).trim();
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded, StandardCharsets.UTF_8);

        String password = "";
        final String[] values = credentials.split(":", 2);
        String username = values[0];
        if (values.length > 1) {
          password = values[1];
        }

        if (!basicPassword.equals(password)) {
          return null;
        }

        final UserDetails userDetails = new UserDetails(username, username, "");

        if (username != null) {
          final List<GrantedAuthority> authorities = new ArrayList<>();
          final UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(username, password, authorities);
          authToken.setDetails(userDetails);

          return authToken;

        }
        return null;

      }

    } else if (request.getHeader(WEBHEADER) != null) {

      String userId = null;
      if (request.getHeader(WEBHEADER) != null) {
        userId = request.getHeader(WEBHEADER);
      }

      String firstName = null;
      if (request.getHeader("X-WEBAUTH-FNAME") != null) {
        firstName = request.getHeader("X-WEBAUTH-FNAME");
      }

      String lastName = null;
      if (request.getHeader("X-WEBAUTH-LNAME") != null) {
        lastName = request.getHeader("X-WEBAUTH-LNAME");
      }

      firstName = santaize(firstName);
      lastName = santaize(lastName);

      final UserDetails userDetails = new UserDetails(userId, firstName, lastName);

      if (userId != null) {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        final UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);
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
