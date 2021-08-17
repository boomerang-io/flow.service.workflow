package io.boomerang.security.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import io.boomerang.mongo.model.TokenScope;
import io.boomerang.security.service.UserDetailsService;

public class SecurityInterceptor implements HandlerInterceptor {

  private UserDetailsService userDetailsService;

  public SecurityInterceptor(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (handler instanceof HandlerMethod) {
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      AuthenticationScope scope = handlerMethod.getMethod().getAnnotation(AuthenticationScope.class);
      if (scope == null) {
        return true;
      }
      TokenScope[] scopes = scope.scopes();
      TokenScope currentScope = userDetailsService.getCurrentScope();
      if (!ArrayUtils.contains(scopes, currentScope)) {
        response.getWriter().write("");
        response.setStatus(401);
        return false;
      }
      return true;
    } else {
      return true;
    }
  }
}
