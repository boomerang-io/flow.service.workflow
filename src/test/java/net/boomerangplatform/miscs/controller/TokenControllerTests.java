package net.boomerangplatform.miscs.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import net.boomerangplatform.controller.TokenController;
import net.boomerangplatform.misc.FlowTests;
import net.boomerangplatform.model.CreateTeamTokenRequest;
import net.boomerangplatform.model.CreateTokenRequest;
import net.boomerangplatform.model.Token;
import net.boomerangplatform.model.TokenResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
public class TokenControllerTests extends FlowTests {
  
  @Autowired
  private TokenController tokenController;

  @Test
  public void testCreatingGlobalToken() {
    
    CreateTokenRequest request = new CreateTokenRequest();
    request.setDescription("Sample token");
    request.setExpiryDate(new Date());
    
    TokenResponse response = tokenController.createNewGlobalToken(request);
    assertNotNull(response.getTokenValue());
    
    String tokenValue = response.getTokenValue();
    System.out.println(tokenValue);
    
    List<Token> tokens = tokenController.getAllGlobalTokens();
    
    assertEquals(1, tokens.size());
    
    Token token = tokens.get(0);
    assertEquals(token.getDescription(), "Sample token");
  }
  
  @Test
  public void testCreatingTeamToken() {
    
    CreateTeamTokenRequest request = new CreateTeamTokenRequest();
    request.setDescription("Team token");
    request.setExpiryDate(new Date());
    request.setTeamId("12345");
    
    TokenResponse response = tokenController.createNewTeamToken(request);
    assertNotNull(response.getTokenValue());
    
    String tokenValue = response.getTokenValue();
    System.out.println(tokenValue);
    
    List<Token> tokens = tokenController.getTokensForTeam("12345");
    
    assertEquals(1, tokens.size());
    
    Token token = tokens.get(0);
    assertEquals(token.getDescription(), "Team token");
  }
}
