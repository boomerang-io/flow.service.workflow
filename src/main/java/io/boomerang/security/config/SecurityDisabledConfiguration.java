package io.boomerang.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true)
@ConditionalOnProperty(name = "flow.authorization.enabled", havingValue = "false")
public class SecurityDisabledConfiguration {
  
  @Bean
  SecurityFilterChain unauthenticatedFilterChain(HttpSecurity http) throws Exception {
    return http.csrf().disable().authorizeHttpRequests((authz) -> authz.anyRequest().permitAll()).build();
  }
  
}
