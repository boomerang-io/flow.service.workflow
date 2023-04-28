package io.boomerang;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;

@SpringBootApplication
@EnableScheduling
@EnableWebSecurity
@OpenAPIDefinition(info = @Info(title = "Boomerang Flow", version = "4.0", description = "Cloud-native Workflow Automation"))
public class Application {
  
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  public OpenAPI api() {
    return new OpenAPI();
  }
}
