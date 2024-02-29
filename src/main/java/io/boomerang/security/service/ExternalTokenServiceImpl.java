package io.boomerang.security.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;

@Service
public class ExternalTokenServiceImpl implements ExternalTokenService {

  @Value("${api.token:boomerangsecuritytokenvalid12345}")
  private String apiToken;

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
}
