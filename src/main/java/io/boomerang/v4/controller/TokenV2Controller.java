package io.boomerang.v4.controller;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.CreateTeamTokenRequest;
import io.boomerang.model.CreateTokenRequest;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.Token;
import io.boomerang.model.TokenResponse;
import io.boomerang.mongo.service.FlowTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v2/token")
public class TokenV2Controller {
  
  @Autowired
  private FlowTokenService tokenService;
  
  @GetMapping(value = "/team/{teamId}")
  public List<Token> getTokensForTeam(@PathVariable String teamId) {
    return tokenService.findAllTeamTokens(teamId);
  }
  
  @GetMapping(value = "/global")
  public List<Token> getAllGlobalTokens() {
    return tokenService.findAllGlobalTokens();
  }

  @DeleteMapping(value = "/{tokenId}")
  public void deleteTeamProperty(@PathVariable String tokenId) {
    tokenService.deleteToken(tokenId);
  }
  
  @PostMapping(value = "/global")
  public TokenResponse createNewGlobalToken(
      @RequestBody CreateTokenRequest request) {
    Date expiryDate = request.getExpiryDate();
    String description = request.getDescription();
    
    return tokenService.createSystemToken(expiryDate, description);
  }
  
  @PostMapping(value = "/team")
  public TokenResponse createNewTeamToken(
      @RequestBody CreateTeamTokenRequest request) {
    Date expiryDate = request.getExpiryDate();
    String description = request.getDescription();
    String teamId = request.getTeamId();
    
    return tokenService.createTeamToken(teamId, expiryDate, description);
  }
  
  @PostMapping(value = "/workflow{workflowId}/token")
  @Operation(summary = "Create a Workflow Token")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public GenerateTokenResponse createToken(@PathVariable String id, @RequestParam String label) {
    return workflowService.generateTriggerToken(id, label);
  }

  @DeleteMapping(value = "/{workflowId}/token")
  @Operation(summary = "Delete a Workflow Token")
  @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void deleteToken(@PathVariable String id, @RequestParam String label) {
    workflowService.deleteToken(id, label);
  }
}
