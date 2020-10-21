package net.boomerangplatform.security.config;

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
import net.boomerangplatform.security.filters.BoomerangAuthorizationFilter;
import net.boomerangplatform.security.service.ApiTokenService;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, proxyTargetClass = true)
public class WebSecurity extends WebSecurityConfigurerAdapter {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String INFO = "/info";

  private static final String API_DOCS = "/api-docs";

  private static final String HEALTH = "/health";

  private static final String INTERNAL = "/internal/**";

  @Autowired
  private ApiTokenService tokenService;

  @Value("${boomerang.authorization.enabled:false}")
  private boolean boomerangAuthorization;

  @Autowired
  @Value("${boomerang.authorization.basic.password:}")
  private String basicPassword;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    if (boomerangAuthorization) {
      setupJWT(http, false, null);
    } else {
      setupNone(http);
    }
  }

  /**
   * Protect end points with JWT checks, optionally checking if signature is valid.
   */
  private void setupJWT(HttpSecurity http, boolean checkSignature, String jwtSecret)
      throws Exception {
    LOGGER.info("Enabling JWT identity checking.");
    final BoomerangAuthorizationFilter jwtFilter = new BoomerangAuthorizationFilter(tokenService,
        authenticationManager(), jwtSecret, checkSignature, basicPassword);
    http.csrf().disable().authorizeRequests().antMatchers(HEALTH, API_DOCS, INFO, INTERNAL)
        .permitAll().and().authorizeRequests().anyRequest().authenticated().and()
        .addFilterBefore(jwtFilter, BasicAuthenticationFilter.class).sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

  /** Disable all spring security. */
  private void setupNone(HttpSecurity http) throws Exception {
    http.csrf().disable().anonymous().authorities(AuthorityUtils.createAuthorityList("ROLE_admin"));
  }

}
