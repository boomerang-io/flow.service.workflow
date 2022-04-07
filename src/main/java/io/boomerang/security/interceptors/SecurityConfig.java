package io.boomerang.security.interceptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.boomerang.service.UserIdentityService;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {
  
  @Autowired
  private UserIdentityService userDetailsService;
  
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
     registry.addInterceptor(new SecurityInterceptor(userDetailsService));
  }

}
