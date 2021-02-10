package net.boomerangplatform;

import java.time.Clock;
import java.util.concurrent.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.github.alturkovic.lock.mongo.configuration.EnableMongoDistributedLock;

@SpringBootApplication
@EnableScheduling
@EnableAsync(proxyTargetClass = true)
@EnableMongoDistributedLock
public class Application {


  private static final Logger LOGGER = LogManager.getLogger();
  
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }
  
  @Bean(name = "flowAsyncExecutor")
  public Executor getCiExecutor() {
    int maxThreads = 200;
    int maxQueue = 100000;
    
    LOGGER.info("Creating task executor service: (max concurrent threads: %d) (max queue: %d)",
        maxThreads, maxQueue);

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(maxThreads);
    executor.setMaxPoolSize(maxThreads);
    executor.setQueueCapacity(maxQueue);

    executor.setThreadNamePrefix("WorfklowServiceExecutor-");
    executor.initialize();
    return executor;
  }

}
