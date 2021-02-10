package net.boomerangplatform;

import java.time.Clock;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.github.alturkovic.lock.mongo.configuration.EnableMongoDistributedLock;
import net.boomerangplatform.service.refactor.TaskServiceImpl;

@SpringBootApplication
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@EnableMongoDistributedLock
public class Application {

  private static final Logger LOGGER = LogManager.getLogger(Application.class);
  
  @Value("${server.tomcat.max-threads}")
  private Integer tomcatMaxThreads;
  
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean(name = "flowAsyncExecutor")
  public Executor getFlowExecutor() {

    int maxQueue = 1000000;
    int maxThreads = 100;

    LOGGER.info("Creating task executor service: (max concurrent threads: %d) (max queue: %d)",
        maxThreads, maxQueue);
    
    LOGGER.info("Tomcat max threads: " +  tomcatMaxThreads);
   
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(maxThreads);
    executor.setMaxPoolSize(maxThreads);
    executor.setQueueCapacity(maxQueue);

    executor.setThreadNamePrefix("FlowServiceExecutor-");
    executor.initialize();
    return executor;
  }
}
