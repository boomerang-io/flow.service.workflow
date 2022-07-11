package io.boomerang.security.filters;

import java.io.IOException;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.slack.api.app_backend.SlackSignature.Generator;
import com.slack.api.app_backend.SlackSignature.Verifier;
import io.boomerang.mongo.service.FlowSettingsService;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SlackSignatureVerificationFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LogManager.getLogger();

  private FlowSettingsService flowSettingsService;
  
  public SlackSignatureVerificationFilter(FlowSettingsService tokenService) {
    this.flowSettingsService = tokenService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    LOGGER.debug("Begin SlackSignatureVerificationFilter()");
    String body = request.getReader().lines().collect(Collectors.joining());
    LOGGER.debug("Body: " + body);
    String signature = request.getHeader("X-Slack-Signature");
    LOGGER.debug("Signature: " + signature);
    String timestamp = request.getHeader("X-Slack-Request-Timestamp");
    LOGGER.debug("Timestamp: " + timestamp);
    
    if (!verifySignature(signature, timestamp, body)) {
      LOGGER.error("Fail SlackSignatureVerificationFilter()");
      response.sendError(401);
      return;
    }

    filterChain.doFilter(request, response);
  }
  
  /*
   * Utlity method for verifying requests are signed by Slack
   * 
   * <h4>Specifications</h4>
   * <ul>
   * <li><a href="https://api.slack.com/authentication/verifying-requests-from-slack">Verifying Requests from Slack</a></li>
   * </ul>
   */
  private Boolean verifySignature(String signature, String timestamp, String body) {
    String key = this.flowSettingsService.getConfiguration("extensions", "slack.signingSecret").getValue();
    LOGGER.debug("Key: " + key);
    LOGGER.debug("Slack Timestamp: " + timestamp);
    LOGGER.debug("Slack Body: " + body);
    Generator generator = new Generator(key);
    Verifier verifier = new Verifier(generator);
    LOGGER.debug("Slack Signature: " + signature);
    LOGGER.debug("Computed Signature: " + generator.generate(timestamp, body));
    return verifier.isValid(timestamp, body, signature);
//    LOGGER.debug("Slack Timestamp: " + timestamp);
//    LOGGER.debug("Slack Body: " + body);
//    String algorithm = "HmacSHA256";
//    String data = "v0:"+ timestamp + ":" + body;
//    HmacUtils hml = new HmacUtils(algorithm, key);
//    String newSignature = "v0=" + hml.hmacHex(data);
//    LOGGER.debug("Slack Signature: " + signature);
//    LOGGER.debug("Computed Signature: " + newSignature);
//    return signature.equals(newSignature);
  }

}
