package net.boomerangplatform.controller.users;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import net.boomerangplatform.client.model.UserProfile;
import net.boomerangplatform.model.OneTimeCode;
import net.boomerangplatform.mongo.entity.FlowUserEntity;
import net.boomerangplatform.service.UserIdentityService;

@RestController
@RequestMapping("/workflow/users")
@ConditionalOnProperty(value = "boomerang.standalone", havingValue = "true", matchIfMissing = false)
public class UsersController {

  @Autowired
  @Qualifier("internalRestTemplate")
  private RestTemplate restTemplate;

  @Autowired
  private UserIdentityService userIdentiyService;
  

  @GetMapping(value = "/profile")
  public ResponseEntity<UserProfile> getUserWithEmail() {
    try {
      FlowUserEntity currentUser = userIdentiyService.getOrRegisterCurrentUser();
      if (currentUser != null) {
        UserProfile userProfile = new UserProfile();
        BeanUtils.copyProperties(currentUser, userProfile);
        return new ResponseEntity<>(userProfile, HttpStatus.OK);
      } else {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } catch (HttpClientErrorException e) {
      return new ResponseEntity<>(e.getStatusCode());
    }
  }
  
  @PutMapping(value = "/register")
  public ResponseEntity<Boolean> register(@RequestBody(required = false) OneTimeCode otc) {
    return userIdentiyService.activateSetup(otc);
  }
}
