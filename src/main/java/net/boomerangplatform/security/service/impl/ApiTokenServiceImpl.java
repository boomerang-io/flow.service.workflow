package net.boomerangplatform.security.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import net.boomerangplatform.security.service.ApiTokenService;

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
}
