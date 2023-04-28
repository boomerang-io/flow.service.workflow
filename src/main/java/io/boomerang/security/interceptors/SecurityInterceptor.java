package io.boomerang.security.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenAccessScope;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenResponse;
import io.boomerang.security.service.TokenService;

public class SecurityInterceptor implements HandlerInterceptor {

  private TokenService tokenService;

  public SecurityInterceptor(TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (handler instanceof HandlerMethod) {

      String header = request.getHeader("x-access-token");

      if (header == null || header.isBlank()) {
        response.getWriter().write("");
        response.setStatus(403);
        return false;
      }
      
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      AuthScope scope = handlerMethod.getMethod().getAnnotation(AuthScope.class);
      if (scope == null) {
        return true;
      }

      TokenObject tokenObject = scope.object();
      TokenAccess tokenAccess = scope.access();

      TokenResponse requestToken = this.tokenService.getToken(header);
      boolean validRequest = false;

      for (TokenAccessScope s : requestToken.getScopes()) {
        if (s.getAccess() == tokenAccess && s.getObject() == tokenObject) {
          validRequest = true;
          break;
        }
      }

      if (!validRequest) {
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
