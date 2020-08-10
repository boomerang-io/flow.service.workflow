package net.boomerangplatform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.error.BoomerangError;
import net.boomerangplatform.error.BoomerangException;

@RestController
public class ErrorController {
  
  @GetMapping("/custom")
  public String customException() {
    throw new BoomerangException(100, "TEAM_NAME_ALREADY_EXISTS", HttpStatus.BAD_REQUEST);
  }
  
  @GetMapping("/enum")
  public String enumExample() {
    throw new BoomerangException(BoomerangError.TEAM_NAME_ALREADY_EXISTS);
  }

}