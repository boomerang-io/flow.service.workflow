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
import io.boomerang.security.service.IdentityService;

/*
 * Interceptor for AuthScope protected controller methods
 * 
 * Presumes endpoint has been through the AuthFilter and SecurityContext is loaded
 */
public class SecurityInterceptor implements HandlerInterceptor {
  
  private static final Logger LOGGER = LogManager.getLogger();
  
  private IdentityService identityService;

  public SecurityInterceptor(IdentityService identityService) {
    this.identityService = identityService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (handler instanceof HandlerMethod) {      
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      AuthScope authScope = handlerMethod.getMethod().getAnnotation(AuthScope.class);
      if (authScope == null) {
        // No annotation found - route does not need authZ
        return true;
      }

      LOGGER.debug("Current Interceptor Scope: " + identityService.getCurrentScope());
      if (identityService.getCurrentScope() == null) {
        // If annotation is found but CurrentScope is not then mismatch must have happened between routes with AuthN and AuthZ
        // TODO set this to return false
//        response.getWriter().write("");
//        response.setStatus(401);
        return true;
      }

      TokenObject tokenObject = authScope.object();
      TokenAccess tokenAccess = authScope.access();

      Token requestToken = this.identityService.getCurrentIdentity();
      boolean validRequest = false;

      for (TokenScope s : requestToken.getScopes()) {
        if (s.getAccess() == tokenAccess && s.getObject() == tokenObject) {
          validRequest = true;
          break;
        }
      }

      if (!validRequest) {
//        response.getWriter().write("");
//        response.setStatus(401);
        return true;
      }
      return true;
    } else {
      return true;
    }
  }
}
