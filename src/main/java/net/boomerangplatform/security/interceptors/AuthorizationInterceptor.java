package net.boomerangplatform.security.interceptors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class AuthorizationInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
          throws Exception {
    System.out.println("AM i here");
    
    HandlerMethod handlerMethod = (HandlerMethod) handler;
    AuthenticationScope scope = handlerMethod.getMethod().getAnnotation(AuthenticationScope.class);
    /* Scopes annotation is necessary. */
    if (scope == null) {
        return false;
    }
    
    Scope[] scopes = scope.scopes();
    
    /** Perform check on token. **/
    
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
          @Nullable ModelAndView modelAndView) throws Exception {
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
          @Nullable Exception ex) throws Exception {
  }

}
 