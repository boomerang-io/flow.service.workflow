package io.boomerang.security.interceptors;

import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.PermissionAction;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.security.model.AuthType;
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
      LOGGER.debug("In SecurityInterceptor()");
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      AuthScope authScope = handlerMethod.getMethod().getAnnotation(AuthScope.class);
      if (authScope == null) {
        LOGGER.debug("SecurityInterceptor: Skipping Authorization");
        // No annotation found - route does not need authZ
        return true;
      }

      LOGGER.debug("SecurityInterceptor Scope: " + identityService.getCurrentScope());
      if (identityService.getCurrentScope() == null) {
        LOGGER.error("SecurityInterceptor - mismatch between AuthN and AuthZ. A permitAll route has an AuthScope.");
        // If annotation is found but CurrentScope is not then mismatch must have happened between routes with AuthN and AuthZ
        // TODO set this to return false
        response.getWriter().write("");
        response.setStatus(401);
        return false;
      }

      AuthType[] requiredTypes = authScope.types();
      Token accessToken = this.identityService.getCurrentIdentity();
      // Check the required level of token is present
      if (!Arrays.asList(requiredTypes).contains(accessToken.getType())) {
        LOGGER.error("SecurityInterceptor - Unauthorized Type / Level. Needed: {}, Provided: {}", requiredTypes.toString(), accessToken.getType().toString());
     // TODO set this to return false
//      response.getWriter().write("");
//      response.setStatus(401);
        return true;
      }
      PermissionScope requiredScope = authScope.scope();
      PermissionAction requiredAccess = authScope.action();
      String requiredRegex = "(\\*{2}|" + requiredScope.getLabel() + ")\\/(\\*{2})\\/(\\*{2}|" + requiredAccess.getLabel() + "){1}";
      LOGGER.debug("SecurityInterceptor - Permission needed: {}, Provided: {}", requiredScope.getLabel() + "/**/" + requiredAccess.getLabel(), accessToken.getPermissions().toString());
      if (!accessToken.getPermissions().stream().anyMatch(p -> (p.matches(requiredRegex)))) {
        LOGGER.error("SecurityInterceptor - Unauthorized Permission.");
        // TODO set this to return false
//      response.getWriter().write("");
//      response.setStatus(401);
      return true;
//      }
//      for (TokenPermission p : accessToken.getPermissions()) {
//        if (p.access() == requiredAccess && p.Object() == requiredObject) {
//          validRequest = true;
//          break;
//        }
      }
      return true;
    } else {
      return true;
    }
  }
}
