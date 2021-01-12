package net.boomerangplatform;

import java.time.Clock;
import org.quartz.spi.JobFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import com.github.alturkovic.lock.mongo.configuration.EnableMongoDistributedLock;
import net.boomerangplatform.scheduler.AutowiringSpringBeanJobFactory;

@SpringBootApplication
@EnableScheduling
@EnableAsync(proxyTargetClass=true)
@EnableMongoDistributedLock
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
}
