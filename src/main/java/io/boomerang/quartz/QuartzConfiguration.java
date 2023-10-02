package io.boomerang.quartz;

import java.io.IOException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import io.boomerang.config.MongoConfiguration;

@Configuration
@EnableScheduling
public class QuartzConfiguration {

  private final Logger logger = LogManager.getLogger(getClass());

  @Value("${spring.data.mongodb.uri}")
  private String mongoUri;
  
  @Autowired
  private MongoConfiguration mongoConfiguration;

  @Autowired
  ApplicationContext applicationContext;
  
  @Bean
  public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
    SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
    scheduler.setApplicationContextSchedulerContextKey("applicationContext");
    scheduler.setWaitForJobsToCompleteOnShutdown(true);
    scheduler.setQuartzProperties(quartzProperties());
    return scheduler;
  }

  private Properties quartzProperties() throws IOException {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean.setLocation(new ClassPathResource("quartz.properties"));
    propertiesFactoryBean.afterPropertiesSet();
    final Properties prop = propertiesFactoryBean.getObject();
    prop.setProperty("org.quartz.jobStore.mongoUri", mongoUri);
    
    String collectionNamePrefix = mongoConfiguration.collectionPrefix();
    if (collectionNamePrefix.endsWith("_")) {
      collectionNamePrefix = collectionNamePrefix.substring(0, collectionNamePrefix.length()-1);
    }
    prop.setProperty("org.quartz.jobStore.collectionPrefix", collectionNamePrefix);
    logger.debug("Quartz Configuration: " + prop.toString());
    
    return prop;
  }
}
