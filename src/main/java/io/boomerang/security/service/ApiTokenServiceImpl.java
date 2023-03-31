package io.boomerang.security.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.model.WorkflowToken;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;

@Service
public class ApiTokenServiceImpl implements ApiTokenService {

  @Value("${api.token:boomerangsecuritytokenvalid12345}")
  private String apiToken;

  private final ThreadLocal<String> tokenMap = new ThreadLocal<>(); // NOSONAR

  @Override
  @NoLogging
  public String createJWTToken() {
    final Date expiryDate = getFutureDate();
    final String subject = "boomerang@us.ibm.com";
    return createToken(subject, expiryDate);
  }

  @Override
  @NoLogging
  public String createJWTToken(String email) {
    final Date expiryDate = getFutureDate();
    return createToken(email, expiryDate);
  }

  @NoLogging
  private String createToken(String subject, Date expiryDate) {
    String jwt;
    try {
      Key key = Keys.hmacShaKeyFor(apiToken.getBytes(StandardCharsets.UTF_8));
      jwt = Jwts.builder().claim("email", subject).setExpiration(expiryDate)
          .signWith(key, SignatureAlgorithm.HS256).compact();
    } catch (InvalidKeyException e) {
      return null;
    }
    return jwt;
  }

  @NoLogging
  private Date getFutureDate() {
    final Calendar now = Calendar.getInstance();
    now.add(Calendar.MINUTE, 10);
    return now.getTime();
  }

  @Override
  @NoLogging
  public String getToken(boolean encoded) {
    if (encoded) {
      return sha1(apiToken);
    }
    return apiToken;
  }

  @Override
  @NoLogging
  public String getUserToken() {
    return tokenMap.get();
  }

  @NoLogging
  private String sha1(String input) {
    String sha1 = null;

    try {
      final MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
      msdDigest.update(input.getBytes(StandardCharsets.UTF_8), 0, input.length());
      sha1 = Hex.encodeHexString(msdDigest.digest());
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
    return sha1;
  }

  @Override
  @NoLogging
  public void storeUserToken(String token) {
    tokenMap.set(token);
  }
  
  /*
   * TODO: convert Workflow Tokens over to the same as all other Tokens.
   */
  
  @Override
  public GenerateTokenResponse generateWorkflowToken(String id, String label) {
    GenerateTokenResponse tokenResponse = new GenerateTokenResponse();
    WorkflowEntity entity = workflowRepository.getWorkflow(id);
    List<WorkflowToken> tokens = entity.getTokens();
    if (tokens == null) {
      tokens = new LinkedList<>();
      entity.setTokens(tokens);
    }
    WorkflowToken newToken = new WorkflowToken();
    newToken.setLabel(label);
    newToken.setToken(createUUID());
    tokens.add(newToken);
    workflowRepository.saveWorkflow(entity);

    tokenResponse.setToken(newToken.getToken());

    return tokenResponse;
  }

  @Override
  public ResponseEntity<HttpStatus> validateWorkflowToken(String id,
      GenerateTokenResponse tokenPayload) {
    WorkflowEntity workflow = workflowRepository.getWorkflow(id);
    if (workflow != null) {
      setupTriggerDefaults(workflow);
      String token = tokenPayload.getToken();
      WorkflowToken workflowToken = workflow.getTokens().stream()
          .filter(customer -> token.equals(customer.getToken())).findAny().orElse(null);
      if (workflowToken != null) {
        return ResponseEntity.ok(HttpStatus.OK);
      }
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
  }

  private String createUUID() {
    try {
      MessageDigest salt = MessageDigest.getInstance("SHA-256");
      salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
      return bytesToHex(salt.digest());
    } catch (NoSuchAlgorithmException e) {
      return null;     
    }
  }
  
  private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

  private static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
  
  @Override
  public void deleteWorkflowToken(String id, String label) {
    WorkflowEntity entity = workflowRepository.getWorkflow(id);
    List<WorkflowToken> tokens = entity.getTokens();
    if (tokens == null) {
      tokens = new LinkedList<>();
      entity.setTokens(tokens);
    }

    WorkflowToken token = tokens.stream().filter(customer -> label.equals(customer.getLabel()))
        .findAny().orElse(null);

    if (token != null) {
      tokens.remove(token);
    }

    workflowRepository.saveWorkflow(entity);
  }
}
