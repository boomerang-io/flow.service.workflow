package io.boomerang.security.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import io.boomerang.mongo.service.FlowTokenService;
import io.boomerang.security.filters.FlowAuthorizationFilter;
import io.boomerang.v4.service.UserService;
import io.boomerang.v4.service.SettingsService;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true)
public class WebSecurity extends WebSecurityConfigurerAdapter {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String INFO = "/info";

  private static final String API_DOCS = "/apis/docs/**";

  private static final String HEALTH = "/health";

  private static final String INTERNAL = "/internal/**";

  private static final String WEBJARS = "/webjars/**";

  private static final String SLACK_INSTALL = "/apis/v1/extensions/slack/install";

  @Value("${jwt.secret:secret}")
  private String jwtSecret;

  @Autowired
  private FlowTokenService tokenService;
  
  @Autowired
  private UserService flowUserService;
  
  @Autowired
  private SettingsService flowSettingsService;

  @Value("${boomerang.authorization.enabled:false}")
  private boolean boomerangAuthorization;

  @Value("${boomerang.authorization.basic.password:}")
  private String basicPassword;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    if (boomerangAuthorization) {
      setupJWT(http);
    } else {
      setupNone(http);
    }
  }

  private void setupJWT(HttpSecurity http)
      throws Exception {
    final FlowAuthorizationFilter jwtFilter = new FlowAuthorizationFilter(tokenService,
        authenticationManager(), flowUserService, flowSettingsService, basicPassword);
    http.csrf().disable().authorizeRequests().antMatchers(HEALTH, API_DOCS, INFO, INTERNAL, WEBJARS, SLACK_INSTALL)
        .permitAll().and().authorizeRequests().anyRequest().authenticated().and()
        .addFilterBefore(jwtFilter, BasicAuthenticationFilter.class).sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

  private void setupNone(HttpSecurity http) throws Exception {
    http.csrf().disable().anonymous().authorities(AuthorityUtils.createAuthorityList("ROLE_admin"));
  }

}
