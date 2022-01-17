package io.boomerang.quartz;

import java.io.IOException;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import io.boomerang.mongo.service.MongoConfiguration;

@Configuration
public class QuartzConfiguration {

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
    prop.setProperty("org.quartz.jobStore.collectionPrefix", collectionNamePrefix);
    
    return prop;
  }
}
