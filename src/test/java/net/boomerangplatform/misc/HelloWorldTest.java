package net.boomerangplatform.misc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
public class HelloWorldTest {
  
  @Test
  public void hello() {
    
  }

}
