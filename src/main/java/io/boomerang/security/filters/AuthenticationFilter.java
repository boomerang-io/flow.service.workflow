package io.boomerang.security.filters;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.filter.OncePerRequestFilter;
import com.slack.api.app_backend.SlackSignature.Generator;
import com.slack.api.app_backend.SlackSignature.Verifier;
import io.boomerang.security.AuthorizationException;
import io.boomerang.security.model.Token;
import io.boomerang.security.service.TokenService;
import io.boomerang.service.SettingsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.impl.DefaultJwtParser;

/*
 * The Filter ensures that the user is Authenticated prior to the Interceptor which validates
 * Authorization
 * 
 * Note: This cannot be auto marked as a Service/Component that Spring Boot would auto inject as
 * then it will apply to all routes
 */
@ConditionalOnProperty(name = "flow.authorization.enabled", havingValue = "true")
public class AuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String X_FORWARDED_USER = "x-forwarded-user";
  private static final String X_FORWARDED_EMAIL = "x-forwarded-email";
  private static final String X_ACCESS_TOKEN = "x-access-token";
  private static final String TOKEN_URL_PARAM_NAME = "access_token";
  private static final String AUTHORIZATION_HEADER = "Authorization";
  private static final String X_SLACK_SIGNATURE = "X-Slack-Signature";
  private static final String X_SLACK_TIMESTAMP = "X-Slack-Request-Timestamp";
  private static final String PATH_ACTIVATE = "/api/v2/activate";
  private static final String PATH_PROFILE = "/api/v2/user/profile";
  private static final String TOKEN_PATTERN = "Bearer\\sbf._(.)+";

  private TokenService tokenService;
  private SettingsService settingsService;
  private String basicPassword;

  public AuthenticationFilter(TokenService tokenService, SettingsService settingsService,
      String basicPassword) {
    super();
    this.tokenService = tokenService;
    this.settingsService = settingsService;
    this.basicPassword = basicPassword;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
      FilterChain chain) throws IOException, ServletException {
    LOGGER.debug("In AuthFilter()");
    try {
      // MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest(req);
      Authentication authentication = null;

      if (req.getHeader(AUTHORIZATION_HEADER) != null) {
        if (req.getHeader(AUTHORIZATION_HEADER).matches(TOKEN_PATTERN)) {
          authentication = getTokenAuthentication(req.getHeader(AUTHORIZATION_HEADER));
        } else {
          authentication = getUserSessionAuthentication(req);
        }
      } else if (req.getHeader(X_ACCESS_TOKEN) != null) {
        //TODO - deprecate this form of header an only rely on AUTHORIZATION_HEADER
        authentication = getTokenAuthentication(req.getHeader(X_ACCESS_TOKEN));
      } else if (req.getParameter(TOKEN_URL_PARAM_NAME) != null) {
        authentication = getTokenAuthentication(req.getParameter(TOKEN_URL_PARAM_NAME));
      } else if (req.getHeader(X_FORWARDED_EMAIL) != null) {
        authentication = getGithubUserAuthentication(req);
      } 

      // if (multiReadRequest.getHeader(X_SLACK_SIGNATURE) != null) {
      // InputStream inputStream = multiReadRequest.getInputStream();
      // byte[] body = StreamUtils.copyToByteArray(inputStream);
      // String signature = multiReadRequest.getHeader(X_SLACK_SIGNATURE);
      // String timestamp = multiReadRequest.getHeader(X_SLACK_TIMESTAMP);
      //
      // if (!verifySignature(signature, timestamp, new String(body))) {
      // LOGGER.error("Fail SlackSignatureVerificationFilter()");
      // res.sendError(401);
      // return;
      // }
      // }
      if (authentication != null) {
        LOGGER.debug("AuthFilter() - authorized.");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(req, res);
        return;
      }
      LOGGER.error("AuthFilter() - not authorized.");
      res.sendError(401);
      return;
    } catch (final HttpClientErrorException e2) {
      LOGGER.error(e2);
      res.sendError(e2.getRawStatusCode());
      return;
    } catch (final AuthorizationException e) {
      LOGGER.error(e);
      res.sendError(401);
      return;
    }
  }

  /*
   * Authorization Header Bearer Token
   * 
   * Populated by the app via OAuth2_Proxy
   * 
   * TODO: figure out a way to ensure it comes via the OAuth2_Proxy
   */
  private UsernamePasswordAuthenticationToken getUserSessionAuthentication(HttpServletRequest request) // NOSONAR
  {
    final String token = request.getHeader(AUTHORIZATION_HEADER);

    boolean allowActivation = false;
    if (request.getServletPath().startsWith(PATH_ACTIVATE)) {
      allowActivation = true;
    }
    
    boolean allowUserCreation = false;
    if (request.getServletPath().startsWith(PATH_PROFILE)) {
      allowUserCreation = true;
    }

    if (token.startsWith("Bearer ")) {
      final String jws = token.replace("Bearer ", "");
      Claims claims;
      String withoutSignature = jws.substring(0, jws.lastIndexOf('.') + 1);

      try {
        claims = (Claims) new DefaultJwtParser().parse(withoutSignature).getBody();
      } catch (ExpiredJwtException e) {
        claims = e.getClaims();
      }
      LOGGER.debug("AuthFilter() - claims: " + claims.toString());
      String email = null;
      if (claims.get("emailAddress") != null) {
        email = (String) claims.get("emailAddress");
      } else if (claims.get("email") != null) {
        email = (String) claims.get("email");
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

      firstName = sanitize(firstName);
      lastName = sanitize(lastName);

      if (email != null && !email.isBlank()) {
        final Token userSessionToken =
            tokenService.createUserSessionToken(email, firstName, lastName, allowActivation, allowUserCreation);
        final List<GrantedAuthority> authorities = new ArrayList<>();
        final UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(email, null, authorities);
        authToken.setDetails(userSessionToken);
        return authToken;
      }
      return null;
    } else if (token.startsWith("Basic ")) {
      String base64Credentials =
          request.getHeader(AUTHORIZATION_HEADER).substring("Basic".length()).trim();

      byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
      String credentials = new String(credDecoded, StandardCharsets.UTF_8);

      String password = "";
      final String[] values = credentials.split(":", 2);
      String email = values[0];

      if (values.length > 1) {
        password = values[1];
      }

      if (!basicPassword.equals(password)) {
        return null;
      }

      if (email != null && !email.isBlank()) {
        final Token userSessionToken =
            tokenService.createUserSessionToken(email, null, null, allowActivation, allowUserCreation);
        final List<GrantedAuthority> authorities = new ArrayList<>();
        final UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(email, password, authorities);
        authToken.setDetails(userSessionToken);
        return authToken;
      }
      return null;
    }
    return null;
  }

  /*
   * Validate and Bump GitHub Protected Auth
   * 
   * TODO: what is the value for X_FORWARDED_USER
   */
  private Authentication getGithubUserAuthentication(HttpServletRequest request) {
    boolean allowActivation = false;
    if (request.getServletPath().startsWith(PATH_ACTIVATE)) {
      allowActivation = true;
    }
    
    boolean allowUserCreation = false;
    if (request.getServletPath().startsWith(PATH_PROFILE)) {
      allowUserCreation = true;
    }
    String email = request.getHeader(X_FORWARDED_EMAIL);
    String userName = request.getHeader(X_FORWARDED_USER);
    final Token token =
        tokenService.createUserSessionToken(email, userName, null, allowActivation, allowUserCreation);
    if (email != null && !email.isBlank()) {
      final List<GrantedAuthority> authorities = new ArrayList<>();
      final UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(token.getPrincipal(), null, authorities);
      authToken.setDetails(token);
      return authToken;
    }
    return null;
  }

  /*
   * Validate and hoist Token Based Auth
   * 
   * Handles the token coming from AUTHORIZATION_HEADER, X_ACCESS_TOKEN, or TOKEN_URL_PARAM_NAME in
   * that order
   */
  private Authentication getTokenAuthentication(String accessToken) {
    if (accessToken.startsWith("Bearer ")) {
      accessToken = accessToken.replace("Bearer ", "");
    }
    if (tokenService.validate(accessToken)) {
      Token token = tokenService.get(accessToken);
      if (token != null) {
        final List<GrantedAuthority> authorities = new ArrayList<>();
        final UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(token.getPrincipal(), null, authorities);
        authToken.setDetails(token);
        return authToken;
      }
    }
    return null;
  }

  private String sanitize(String value) {
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

  /*
   * Utlity method for verifying requests are signed by Slack
   * 
   * <h4>Specifications</h4> <ul> <li><a
   * href="https://api.slack.com/authentication/verifying-requests-from-slack">Verifying Requests
   * from Slack</a></li> </ul>
   */
  private Boolean verifySignature(String signature, String timestamp, String body) {
    String key =
        this.settingsService.getSettingConfig("extensions", "slack.signingSecret").getValue();
    LOGGER.debug("Key: " + key);
    LOGGER.debug("Slack Timestamp: " + timestamp);
    LOGGER.debug("Slack Body: " + body);
    Generator generator = new Generator(key);
    Verifier verifier = new Verifier(generator);
    LOGGER.debug("Slack Signature: " + signature);
    LOGGER.debug("Computed Signature: " + generator.generate(timestamp, body));
    return verifier.isValid(timestamp, body, signature);
  }

  @Override
  // TODO figure out why these aren't being applied in the SecurityConfig
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/error") || path.startsWith("/health") || path.startsWith("/api/docs")
        || path.startsWith("/internal");
  }
}
