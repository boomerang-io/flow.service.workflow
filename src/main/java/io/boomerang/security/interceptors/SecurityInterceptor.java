package io.boomerang.security.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenScope;
import io.boomerang.security.service.TokenService;
import io.boomerang.security.service.UserIdentityService;

public class SecurityInterceptor implements HandlerInterceptor {
  
  private static final Logger LOGGER = LogManager.getLogger();

  private TokenService tokenService;
  
  private UserIdentityService userIDService;

  public SecurityInterceptor(TokenService tokenService, UserIdentityService userIDService) {
    this.tokenService = tokenService;
    this.userIDService = userIDService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (handler instanceof HandlerMethod) {

      LOGGER.debug(userIDService.getCurrentScope());
      
      // TODO: shift this to receiving from the Security Context rather than the header
      
      String header = request.getHeader("x-access-token");

      if (header == null || header.isBlank()) {
        response.getWriter().write("");
        response.setStatus(403);
        return true;
      }
      
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      AuthScope scope = handlerMethod.getMethod().getAnnotation(AuthScope.class);
      if (scope == null) {
        return true;
      }

      TokenObject tokenObject = scope.object();
      TokenAccess tokenAccess = scope.access();

      Token requestToken = this.tokenService.get(header);
      boolean validRequest = false;

      for (TokenScope s : requestToken.getScopes()) {
        if (s.getAccess() == tokenAccess && s.getObject() == tokenObject) {
          validRequest = true;
          break;
        }
      }

      if (!validRequest) {
        response.getWriter().write("");
        response.setStatus(401);
        return true;
      }
      return true;
    } else {
      return true;
    }
  }
}
