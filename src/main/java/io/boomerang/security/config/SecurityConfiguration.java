package io.boomerang.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import io.boomerang.security.filters.AuthenticationFilter;
import io.boomerang.security.service.TokenService;
import io.boomerang.v4.service.SettingsService;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true)
@ConditionalOnProperty(name = "flow.authorization.enabled", havingValue = "true",
    matchIfMissing = true)
public class SecurityConfiguration {

  // private static final Logger LOGGER = LogManager.getLogger();

  private static final String INFO = "/info";

  private static final String API_DOCS = "/api/docs/**";

  private static final String HEALTH = "/health";

  private static final String INTERNAL = "/internal/**";

  private static final String WEBJARS = "/webjars/**";

  private static final String SLACK_INSTALL = "/api/v2/extensions/slack/install";

  private static final String API = "/api/**";
  //
  // @Value("${jwt.secret:secret}")
  // private String jwtSecret;
  //

  @Autowired
  private TokenService tokenService;

  @Autowired
  private SettingsService settingsService;
  //
  // @Value("${boomerang.authorization.enabled:false}")
  // private boolean boomerangAuthorization;
  //
  // @Value("${boomerang.authorization.basic.password:}")
  // private String basicPassword;

  // @Bean
  // @Order(1)
  // SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
  // return http.csrf().disable().authorizeRequests().antMatchers(HEALTH, API_DOCS, INFO, INTERNAL,
  // WEBJARS, SLACK_INSTALL).permitAll().and().authorizeRequests().anyRequest()
  // .authenticated().and().addFilterBefore(authFilter,
  // UsernamePasswordAuthenticationFilter.class).build();
  // }

  @Bean
  @Order(2)
  SecurityFilterChain internalAuthFilterChain(HttpSecurity http) throws Exception {
    http.csrf().disable().authorizeRequests()
        .antMatchers(HEALTH, API_DOCS, INFO, INTERNAL, WEBJARS, SLACK_INSTALL).permitAll();
    return http.build();
  }

  @Bean
  @Order(1)
  SecurityFilterChain tokenFilterChain(HttpSecurity http) throws Exception {
    final AuthenticationFilter authFilter = new AuthenticationFilter(tokenService, settingsService);
    http.csrf().disable().authorizeRequests().antMatchers(API).authenticated().and()
        .addFilterBefore(authFilter, BasicAuthenticationFilter.class).sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    return http.build();
  }

  @Bean
  SecurityFilterChain unauthenticatedFilterChain(HttpSecurity http) throws Exception {
    http.csrf().disable().authorizeHttpRequests((authz) -> authz.anyRequest().permitAll());
    return http.build();
  }
  //
}
