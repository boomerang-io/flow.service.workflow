//package io.boomerang.security.config;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.web.servlet.FilterRegistrationBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.Ordered;
//import io.boomerang.mongo.service.FlowSettingsService;
//import io.boomerang.security.filters.SlackSignatureVerificationFilter;
//
//@Configuration
//public class SlackSecurityVerificationFilterConfig {
//
//  @Autowired
//  private FlowSettingsService flowSettingsService;
//
//  private static final String PATH_PATTERN = "/apis/v1/extensions/slack/*";
//  
//  @Bean
//  public FilterRegistrationBean<SlackSignatureVerificationFilter> SlackSignatureVerificationFilterBean()
//  {
//      FilterRegistrationBean<SlackSignatureVerificationFilter> bean = new FilterRegistrationBean<>();
//      bean.setFilter(new SlackSignatureVerificationFilter(flowSettingsService));
//      bean.addUrlPatterns(PATH_PATTERN);
//      bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
//      return bean;
//  }
//
//}
