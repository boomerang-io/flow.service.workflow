package io.boomerang.security.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.mongo.model.UserType;
import io.boomerang.security.model.CreateTokenRequest;
import io.boomerang.security.model.CreateTokenResponse;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenType;
import io.boomerang.security.model.TokenTypePrefix;
import io.boomerang.v4.data.entity.TokenEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.entity.ref.ActionEntity;
import io.boomerang.v4.data.repository.TokenRepository;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;
import io.boomerang.v4.service.RelationshipService;
import io.boomerang.v4.service.UserService;

@Service
public class TokenServiceImpl implements TokenService {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private TokenRepository tokenRepository;

  @Autowired
  private UserService userService;

  @Autowired
  private RelationshipService relationshipService;

  @Value("${flow.token.max-user-session-duration}")
  private Integer MAX_USER_SESSION_TOKEN_DURATION;

  /*
   * Creates an Access Token
   * 
   * Limited to creation by a User on behalf of a User, Workflow, Team, Global scope
   */
  @Override
  public CreateTokenResponse create(CreateTokenRequest request) {
    if (request.getType() == null || request.getName() == null || request.getName().isEmpty()) {
      // TODO make real exception
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    
    if (TokenType.session.equals(request.getType())) {
      // TODO make real exception
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);   
    }

    TokenEntity tokenEntity = new TokenEntity();
    tokenEntity.setType(request.getType());
    tokenEntity.setName(request.getName());
    tokenEntity.setDescription(request.getDescription());
    tokenEntity.setExpirationDate(request.getExpirationDate());
    tokenEntity.setValid(true);
    tokenEntity.setScopes(request.getScopes());

    // TODO set Author based on current scope (should be a User Entity via UI)
    // token.setCreatorId(creatorId);
    // UserEntity user = userService.getUserByID(creatorId);
    // if (user != null) {
    // token.setCreatorName(user.getName());
    // }

    String prefix = TokenTypePrefix.valueOfLabel(request.getType().toString()).toString();
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();

    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
    tokenRepository.save(tokenEntity);

    // Create Authorization Relationship
    RelationshipRef to = RelationshipRef.USER;
    Optional<String> toRef = Optional.empty();
    if (TokenType.global.equals(request.getType())) {
      to = RelationshipRef.GLOBAL;
    } else if (TokenType.team.equals(request.getType())) {
      to = RelationshipRef.TEAM;
      toRef = Optional.of(request.getOwner());
    } else if (TokenType.workflow.equals(request.getType())) {
      to = RelationshipRef.WORKFLOW;
      toRef = Optional.of(request.getOwner());
    } else if (TokenType.user.equals(request.getType())) {
      to = RelationshipRef.USER;
      toRef = Optional.of(request.getOwner()); // Switch to Current Scope
    }
    relationshipService.addRelationshipRef(RelationshipRef.TOKEN, tokenEntity.getId(),
        RelationshipType.AUTHORIZES, to, toRef);
    
    CreateTokenResponse response = new CreateTokenResponse();
    response.setValue(uniqueToken);
    response.setId(tokenEntity.getId());
    response.setType(request.getType());
    response.setExpirationDate(request.getExpirationDate());
    return response;
  }

  public String hashString(String originalString) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();
      for (byte element : hash) {
        String hex = Integer.toHexString(0xff & element);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  @Override
  public boolean validate(String token) {
    String hash = hashString(token);
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findByToken(hash);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      Date currentDate = new Date();
      boolean validToken = tokenEntity.isValid();

      if (validToken && tokenEntity.getExpirationDate().after(currentDate)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean delete(@Valid String id) {
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findById(id);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      tokenEntity.setValid(false);
      return true;
    }
    return false;
  }

  @Override
  public Page<Token> query(Optional<Date> from, Optional<Date> to, Pageable pageable,
      Optional<List<TokenType>> queryTypes) {
    List<Criteria> criterias = new ArrayList<>();

    if (from.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("creationDate").gte(from.get());
      criterias.add(dynamicCriteria);
    }

    if (to.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("creationDate").lte(to.get());
      criterias.add(dynamicCriteria);
    }

    if (queryTypes.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("type").in(queryTypes.get());
      criterias.add(dynamicCriteria);
    }
    Criteria criteria =
        new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()]));
    Query query = new Query(criteria).with(pageable);

    List<TokenEntity> entities = mongoTemplate.find(query.with(pageable), TokenEntity.class);

    List<Token> response = new LinkedList<>();
    entities.forEach(t -> {
      response.add(new Token(t));
    });

    Page<Token> pages = PageableExecutionUtils.getPage(response, pageable,
        () -> mongoTemplate.count(query, ActionEntity.class));

    return pages;
  }

  @Override
  public Token get(String token) {
    String hash = hashString(token);
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findByToken(hash);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      Date currentDate = new Date();
      boolean validToken = tokenEntity.isValid();

      if (validToken && tokenEntity.getExpirationDate().after(currentDate)) {
        Token response = new Token();
        BeanUtils.copyProperties(tokenEntity, response);
        return response;
      }
    }
    return null;
  }

  /*
   * Wraps createUserSessionToken finding the user by Email
   * 
   * Used by the AuthenticationFilter when accessed by non Access Token
   */
  @Override
  public Token createUserSessionToken(String email, String name) {
    UserEntity user = userService.getOrRegisterUser(email, name, UserType.user);
    return createUserSessionToken(user);
  }

  /*
   * Creates a token expiring in MAX SESSION TIME
   * 
   * TODO: add scopes that a user session token would have (needs to based on User ... eventually
   * dynamic)
   * 
   * TODO: is this method declaration ever call besides the wrapper - should they be combined.
   */
  @Override
  public Token createUserSessionToken(UserEntity user) {

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR, MAX_USER_SESSION_TOKEN_DURATION);
    Date expiryDate = cal.getTime();

    TokenEntity tokenEntity = new TokenEntity();
    tokenEntity.setCreationDate(new Date());
    tokenEntity.setDescription("Generated User Session Token");
    tokenEntity.setType(TokenType.session);
    tokenEntity.setExpirationDate(expiryDate);
    tokenEntity.setValid(true);
    tokenEntity.setCreatedBy(user);

    String prefix = TokenTypePrefix.session.label;
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();

    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
    tokenEntity = tokenRepository.save(tokenEntity);
    
    // Create Authorization Relationship
    relationshipService.addRelationshipRef(RelationshipRef.TOKEN, tokenEntity.getId(),
        RelationshipType.AUTHORIZES, RelationshipRef.USER, Optional.of(user.getId()));

    return new Token(tokenEntity);
  }
}
