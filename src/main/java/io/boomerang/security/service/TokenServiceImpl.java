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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.security.model.CreateTokenRequest;
import io.boomerang.security.model.CreateTokenResponse;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenPermission;
import io.boomerang.security.model.TokenScope;
import io.boomerang.security.model.TokenTypePrefix;
import io.boomerang.service.RelationshipService;
import io.boomerang.v4.data.entity.TokenEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.entity.ref.ActionEntity;
import io.boomerang.v4.data.repository.TokenRepository;
import io.boomerang.v4.model.UserType;
import io.boomerang.v4.model.enums.RelationshipRef;
import io.boomerang.v4.model.enums.RelationshipType;

@Service
public class TokenServiceImpl implements TokenService {

  private static final Logger LOGGER = LogManager.getLogger();

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private TokenRepository tokenRepository;

  @Autowired
  private IdentityService identityService;

  @Autowired
  private RelationshipService relationshipService;

  @Value("${flow.token.max-user-session-duration}")
  private Integer MAX_USER_SESSION_TOKEN_DURATION;

  /*
   * Creates an Access Token
   * 
   * Limited to creation by a User on behalf of a User, Workflow, Team, Global scope
   * 
   * TODO: make sure requesting principal has access to create for the provided principal
   */
  @Override
  public CreateTokenResponse create(CreateTokenRequest request) {
    if (request.getType() == null || request.getName() == null || request.getName().isEmpty()) {
      // TODO make real exception
      // TODO add permissions empty array check
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    // Disallow creation of session tokens except via internal AuthenticationFilter
    if (TokenScope.session.equals(request.getType())) {
      // TODO make real exception
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }

    LOGGER.debug("Creating {} token...", request.getType().toString());
    TokenEntity tokenEntity = new TokenEntity();
    // Ensure Principal is provided for all types but global
    if (!TokenScope.global.equals(request.getType())
        && (request.getPrincipal() == null || request.getPrincipal().isEmpty())) {
      // TODO make real exception
      throw new BoomerangException(BoomerangError.WORKFLOW_INVALID_REF);
    }
    if (!TokenScope.global.equals(request.getType())) {
      tokenEntity.setPrincipal(request.getPrincipal());
    }
    tokenEntity.setType(request.getType());
    tokenEntity.setName(request.getName());
    tokenEntity.setDescription(request.getDescription());
    tokenEntity.setExpirationDate(request.getExpirationDate());
    tokenEntity.setPermissions(request.getPermissions());

    String prefix = TokenTypePrefix.valueOf(request.getType().toString()).getPrefix();
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();

    final String hashToken = hashString(uniqueToken);
    LOGGER.debug("Token: " + uniqueToken);
    tokenEntity.setToken(hashToken);
    tokenEntity = tokenRepository.save(tokenEntity);

    // Create an Audit relationship
    // relationshipService.addRelationshipRef(RelationshipRef.USER,
    // identityService.getCurrentUser().getId(),
    // RelationshipType.CREATED, RelationshipRef.TOKEN, Optional.of(tokenEntity.getId()));

    // Create Authorization Relationship
    RelationshipRef to = null;
    Optional<String> toRef = Optional.empty();
    if (TokenScope.global.equals(request.getType())) {
      to = RelationshipRef.GLOBAL;
    } else if (TokenScope.team.equals(request.getType())) {
      to = RelationshipRef.TEAM;
      toRef = Optional.of(request.getPrincipal());
    } else if (TokenScope.workflow.equals(request.getType())) {
      to = RelationshipRef.WORKFLOW;
      toRef = Optional.of(request.getPrincipal());
    } else if (TokenScope.user.equals(request.getType())) {
      to = RelationshipRef.USER;
      toRef = Optional.of(request.getPrincipal());
    }
    relationshipService.addRelationshipRef(RelationshipRef.TOKEN, tokenEntity.getId(),
        RelationshipType.AUTHORIZES, to, toRef);

    CreateTokenResponse tokenResponse = new CreateTokenResponse();
    tokenResponse.setToken(uniqueToken);
    tokenResponse.setId(tokenEntity.getId());
    tokenResponse.setType(request.getType());
    tokenResponse.setExpirationDate(request.getExpirationDate());
    return tokenResponse;
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
    LOGGER.debug("Token Validation - token: " + token);
    String hash = hashString(token);
    LOGGER.debug("Token Validation - hash: " + hash);
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findByToken(hash);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      if (isValid(tokenEntity.getExpirationDate())) {
        LOGGER.debug("Token Validation - valid");
        return true;
      }
    }
    LOGGER.debug("Token Validation - not valid");
    return false;
  }

  @Override
  public boolean delete(@Valid String id) {
    Optional<TokenEntity> tokenEntityOptional = this.tokenRepository.findById(id);
    if (tokenEntityOptional.isPresent()) {
      TokenEntity tokenEntity = tokenEntityOptional.get();
      this.tokenRepository.delete(tokenEntity);
      return true;
    }
    return false;
  }

  @Override
  public Page<Token> query(Optional<Date> from, Optional<Date> to, Optional<Integer> queryLimit,
      Optional<Integer> queryPage, Optional<Direction> queryOrder, Optional<String> querySort,
      Optional<List<TokenScope>> queryTypes, Optional<List<String>> queryPrincipals) {
    Pageable pageable = Pageable.unpaged();
    final Sort sort =
        Sort.by(new Order(queryOrder.orElse(Direction.ASC), querySort.orElse("creationDate")));
    if (queryLimit.isPresent()) {
      pageable = PageRequest.of(queryPage.get(), queryLimit.get(), sort);
    }
    List<Criteria> criteriaList = new ArrayList<>();

    if (from.isPresent() && !to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(from.get());
      criteriaList.add(criteria);
    } else if (!from.isPresent() && to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").lt(to.get());
      criteriaList.add(criteria);
    } else if (from.isPresent() && to.isPresent()) {
      Criteria criteria = Criteria.where("creationDate").gte(from.get()).lt(to.get());
      criteriaList.add(criteria);
    }
    if (queryTypes.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("type").in(queryTypes.get());
      criteriaList.add(dynamicCriteria);
    }
    if (queryPrincipals.isPresent()) {
      Criteria dynamicCriteria = Criteria.where("principal").in(queryPrincipals.get());
      criteriaList.add(dynamicCriteria);
    }

    Criteria[] criteriaArray = criteriaList.toArray(new Criteria[criteriaList.size()]);
    Criteria allCriteria = new Criteria();
    if (criteriaArray.length > 0) {
      allCriteria.andOperator(criteriaArray);
    }
    Query query = new Query(allCriteria);
    if (queryLimit.isPresent()) {
      query.with(pageable);
    } else {
      query.with(sort);
    }

    List<TokenEntity> entities = mongoTemplate.find(query, TokenEntity.class);

    List<Token> response = new LinkedList<>();
    entities.forEach(te -> {
      Token token = new Token(te);
      token.setValid(isValid(te.getExpirationDate()));
      response.add(token);
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
      Token response = new Token();
      response.setValid(isValid(tokenEntity.getExpirationDate()));
      BeanUtils.copyProperties(tokenEntity, response);
      return response;
    }
    return null;
  }

  /*
   * Token.valid element is only on the Model and not the Data Entity It is a derived element based
   * on expiration date
   */
  private boolean isValid(Date expirationDate) {
    Date currentDate = new Date();
    if (expirationDate == null || expirationDate.after(currentDate)) {
      return true;
    }
    return false;
  }

  /*
   * Creates a token expiring in MAX SESSION TIME. Used by the AuthenticationFilter when accessed by
   * non Access Token
   * 
   * TODO: add scopes that a user session token would have (needs to based on User ... eventually
   * dynamic)
   * 
   * TODO: is this method declaration ever call besides the wrapper - should they be combined.
   */
  public Token createUserSessionToken(String email, String firstName, String lastName,
      boolean activateOverride) {
    Optional<UserEntity> user = Optional.empty();
    if (activateOverride && !identityService.isActivated()) {
      user = identityService.getAndRegisterUser(email, firstName, lastName,
          Optional.of(UserType.admin));
    } else if (identityService.isActivated()) {
      user = identityService.getAndRegisterUser(email, firstName, lastName,
          Optional.of(UserType.user));
    } else {
      throw new HttpClientErrorException(HttpStatus.LOCKED);
    }

    if (!user.isPresent()) {
      // TODO throw exception
      return null;
    }
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR, MAX_USER_SESSION_TOKEN_DURATION);
    Date expiryDate = cal.getTime();

    TokenEntity tokenEntity = new TokenEntity();
    tokenEntity.setCreationDate(new Date());
    tokenEntity.setDescription("Generated User Session Token");
    tokenEntity.setType(TokenScope.session);
    tokenEntity.setExpirationDate(expiryDate);
    tokenEntity.setPrincipal(user.get().getId());
    tokenEntity.setPermissions(List.of(TokenPermission.ANY_READ, TokenPermission.ANY_WRITE, TokenPermission.ANY_DELETE, TokenPermission.ANY_ACTION));

    String prefix = TokenTypePrefix.session.prefix;
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();

    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
    tokenEntity = tokenRepository.save(tokenEntity);

    // Create Authorization Relationship
    relationshipService.addRelationshipRef(RelationshipRef.TOKEN, tokenEntity.getId(),
        RelationshipType.AUTHORIZES, RelationshipRef.USER, Optional.of(user.get().getId()));

    return new Token(tokenEntity);
  }
}
