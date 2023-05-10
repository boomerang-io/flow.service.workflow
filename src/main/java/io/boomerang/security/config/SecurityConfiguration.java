package io.boomerang.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import io.boomerang.security.filters.AuthenticationFilter;
import io.boomerang.security.service.TokenService;
import io.boomerang.v4.service.SettingsService;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true)
@ConditionalOnProperty(name = "flow.authorization.enabled", havingValue = "true")
public class SecurityConfiguration {

  private static final String INFO = "/info";

  private static final String API_DOCS = "/api/docs/**";

  private static final String HEALTH = "/health";

  private static final String INTERNAL = "/internal/**";

  private static final String WEBJARS = "/webjars/**";

  private static final String SLACK_INSTALL = "/api/v2/extensions/slack/install";

  @Autowired
  private TokenService tokenService;

  @Autowired
  private SettingsService settingsService;
  
  @Autowired
  @Qualifier("delegatedAuthenticationEntryPoint")
  AuthenticationEntryPoint authEntryPoint;

  @Value("${flow.authorization.basic.password:}")
  private String basicPassword;
  //
  // @Value("${jwt.secret:secret}")
  // private String jwtSecret;
  //
  
  //TODO figure out why the bean order implementation below is not working
  //TODO figure out why we also have to have the permitAll matches in the doNotFilter of AuthenticationFilter
    @Bean
    SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
      final AuthenticationFilter authFilter =
          new AuthenticationFilter(tokenService, settingsService, basicPassword);
      http.csrf().disable().authorizeRequests()
          .antMatchers(HEALTH, API_DOCS, INFO, INTERNAL, WEBJARS, SLACK_INSTALL).permitAll().and()
          .authorizeRequests().anyRequest().authenticated().and()
          .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
          .and()
          .exceptionHandling()
          .authenticationEntryPoint(authEntryPoint);
      return http.build();
    }
//
//  @Bean
//  @Order(1)
//  SecurityFilterChain tokenFilterChain(HttpSecurity http) throws Exception {
//    final AuthenticationFilter authFilter =
//        new AuthenticationFilter(tokenService, settingsService, basicPassword);
//    http.csrf().disable().authorizeRequests().anyRequest().authenticated().and()
//        .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
//        .sessionManagement()
//        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//    return http.build();
//  }
//
// @Bean
// @Order(2)
// SecurityFilterChain unauthenticatedFilterChain(HttpSecurity http) throws Exception {
//   http.csrf().disable().authorizeRequests()
//       .antMatchers(HEALTH, API_DOCS, INFO, INTERNAL, WEBJARS, SLACK_INSTALL).permitAll();
//   return http.build();
// }
}
