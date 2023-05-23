package io.boomerang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.model.CreateTokenRequest;
import io.boomerang.security.model.CreateTokenResponse;
import io.boomerang.security.service.TokenService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/internal/debug")
@Hidden
public class InternalDebugController {

  @Autowired
  private TokenService tokenService;

  @PostMapping("/token")
  @Operation(summary = "Create Token")
  public CreateTokenResponse create(@RequestBody CreateTokenRequest newToken) {
    return tokenService.create(newToken);
  }

//  @GetMapping("token-scopes")
//  public List<TokenTypeResponse> getTokenScopes() {
//
//    List<TokenTypeResponse>  responses = new LinkedList<>();
//
//    for (TokenScope type : TokenScope.values()) {
//      TokenTypeResponse response = new TokenTypeResponse();
//      response.setAccess(type.access());
//      response.setObject(type.object());
//      response.setTypes(type.types());
//
//      responses.add(response);
//
//    }
//    return responses;
//  }
}
