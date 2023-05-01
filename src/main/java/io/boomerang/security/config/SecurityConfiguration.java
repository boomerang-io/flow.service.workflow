package io.boomerang.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import io.boomerang.security.filters.AuthenticationFilter;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true)
public class SecurityConfiguration {

//  private static final Logger LOGGER = LogManager.getLogger();

  private static final String INFO = "/info";

  private static final String API_DOCS = "/api/docs/**";

  private static final String HEALTH = "/health";

  private static final String INTERNAL = "/internal/**";

  private static final String WEBJARS = "/webjars/**";

  private static final String SLACK_INSTALL = "/api/v2/extensions/slack/install";
//
//  @Value("${jwt.secret:secret}")
//  private String jwtSecret;
//  
  @Autowired
  private AuthenticationFilter authFilter;
//
//  @Value("${boomerang.authorization.enabled:false}")
//  private boolean boomerangAuthorization;
//
//  @Value("${boomerang.authorization.basic.password:}")
//  private String basicPassword;

//  @Override
//  protected void configure(HttpSecurity http) throws Exception {
//    if (boomerangAuthorization) {
//      setupJWT(http);
//    } else {
//      setupNone(http);
//    }
//  }
//
//  private void setupJWT(HttpSecurity http)
//      throws Exception {
//    final FlowAuthorizationFilter jwtFilter = new FlowAuthorizationFilter(tokenService,
//        authenticationManager(), flowUserService, flowSettingsService, basicPassword);
//    http.csrf().disable().authorizeRequests().antMatchers(HEALTH, API_DOCS, INFO, INTERNAL, WEBJARS, SLACK_INSTALL)
//        .permitAll().and().authorizeRequests().anyRequest().authenticated().and()
//        .addFilterBefore(jwtFilter, BasicAuthenticationFilter.class).sessionManagement()
//        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//  }
//
//  private void setupNone(HttpSecurity http) throws Exception {
//    http.csrf().disable().anonymous().authorities(AuthorityUtils.createAuthorityList("ROLE_admin"));
//  }

  
  @Bean
  @Order(1)
  SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
    return http.csrf().disable().authorizeRequests().antMatchers(HEALTH, API_DOCS, INFO, INTERNAL, WEBJARS, SLACK_INSTALL).permitAll().and().authorizeRequests().anyRequest()
        .authenticated().and().addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class).build();
  }
  
//  @Bean
//  @Order(2)
//  SecurityFilterChain internalAuthFilterChain(HttpSecurity http) throws Exception {
//    return http.csrf().disable().antMatcher(TOKEN_EXCHANGE_PATH).authorizeRequests().anyRequest()
//        .authenticated().and().addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class).build();
//  }

//  @Bean
//  @Order(1)
//  SecurityFilterChain tokenFilterChain(HttpSecurity http) throws Exception {
//    return http.csrf().disable().antMatcher(API_BASE_PATH).authorizeRequests().anyRequest()
//        .authenticated().and().addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class).build();
//  }

  @Bean
  SecurityFilterChain unauthenticatedFilterChain(HttpSecurity http) throws Exception {
    http.csrf().disable().authorizeHttpRequests((authz) -> authz.anyRequest().permitAll());
    return http.build();
  }
  
}
