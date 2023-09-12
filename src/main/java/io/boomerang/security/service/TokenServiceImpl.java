package io.boomerang.security.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
import io.boomerang.data.entity.RelationshipEntity;
import io.boomerang.data.entity.UserEntity;
import io.boomerang.data.entity.ref.ActionEntity;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.enums.RelationshipRef;
import io.boomerang.model.enums.RelationshipType;
import io.boomerang.model.enums.UserType;
import io.boomerang.security.entity.TokenEntity;
import io.boomerang.security.model.AuthType;
import io.boomerang.security.model.CreateTokenRequest;
import io.boomerang.security.model.CreateTokenResponse;
import io.boomerang.security.model.PermissionScope;
import io.boomerang.security.model.RoleEnum;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenTypePrefix;
import io.boomerang.security.repository.RoleRepository;
import io.boomerang.security.repository.TokenRepository;
import io.boomerang.service.RelationshipService;

@Service
public class TokenServiceImpl implements TokenService {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String TOKEN_PERMISSION_REGEX =
      "(\\*{2}|[0-9a-zA-Z\\-]+)\\/(\\*{2}|[0-9a-zA-Z\\-]+)\\/(\\*{2}|Read|Write|Action|Delete){1}";

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private TokenRepository tokenRepository;

  @Autowired
  private RoleRepository roleRepository;

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
    // Disallow creation of session tokens except via internal AuthenticationFilter
    if (AuthType.session.equals(request.getType())) {
      throw new BoomerangException(BoomerangError.TOKEN_INVALID_SESSION_REQ);
    }

    // Required field checks
    // - Type and name: required for all tokens
    // - Principal: required if type!=global
    // - Permissions: required for type!=user
    // - Teams: require for type=user - this is checked later down
    if (request.getType() == null || request.getName() == null || request.getName().isEmpty()
        || (!AuthType.global.equals(request.getType())
            && (request.getPrincipal() == null || request.getPrincipal().isBlank()))
        || (!AuthType.user.equals(request.getType())
            && (request.getPermissions() == null || request.getPermissions().isEmpty()))) {
      throw new BoomerangException(BoomerangError.TOKEN_INVALID_REQ);
    }

    // Validate permissions matches the REGEX
    if (!AuthType.user.equals(request.getType())) {
      request.getPermissions().forEach(p -> {
        if (!p.matches(TOKEN_PERMISSION_REGEX)) {
          throw new BoomerangException(BoomerangError.TOKEN_INVALID_PERMISSION);
        }
        String[] pSplit = p.split("/");
        LOGGER.debug("Scope: " + PermissionScope.valueOfLabel(pSplit[0]));
        if (PermissionScope.valueOfLabel(pSplit[0]) == null) {
          throw new BoomerangException(BoomerangError.TOKEN_INVALID_PERMISSION);
        }
        LOGGER.debug("Principal: " + pSplit[1].toLowerCase());
        if (pSplit[1] == null) {
          throw new BoomerangException(BoomerangError.TOKEN_INVALID_PERMISSION);
        }
        if (AuthType.team.equals(request.getType())
            || AuthType.workflow.equals(request.getType())) {
          if (!request.getPrincipal().equals(pSplit[1])) {
            throw new BoomerangException(BoomerangError.TOKEN_INVALID_PERMISSION);
          }
        }
        LOGGER.debug("Action: " + pSplit[2]);
        // ACTION is already checked as part of the regex
      });
    }

    // Create TokenEntity
    TokenEntity tokenEntity = new TokenEntity();
    tokenEntity.setType(request.getType());
    tokenEntity.setName(request.getName());
    tokenEntity.setDescription(request.getDescription());
    tokenEntity.setExpirationDate(request.getExpirationDate());
    if (!AuthType.global.equals(request.getType())) {
      tokenEntity.setPrincipal(request.getPrincipal());
    }
    // Set Permissions
    // If type=user then retrieve role permissions for each team on User Token
    // else set to the permissions in the request
    if (AuthType.user.equals(request.getType())) {
      Optional<List<String>> teams = Optional.empty();
      if (request.getTeams() != null && !request.getTeams().isEmpty()) {
        teams = Optional.of(request.getTeams());
      }
      // Validate principal against all the team permissions the user has
      List<RelationshipEntity> userRels = relationshipService.getFilteredRels(
          Optional.of(RelationshipRef.USER), Optional.of(List.of(request.getPrincipal())),
          Optional.of(RelationshipType.MEMBEROF), Optional.of(RelationshipRef.TEAM), teams, false);
      for (RelationshipEntity rel : userRels) {
        String role = RoleEnum.READER.getLabel();
        if (rel.getData() != null && rel.getData().get("role") != null) {
          role = rel.getData().get("role").toString();
        }
        List<String> rolePermissions = roleRepository.findByTypeAndName("team", role).getPermissions();
        List<String> replacedPermissions = rolePermissions.stream()
            .map(str -> str.replace("{principal}", rel.getToRef()))
            .collect(Collectors.toList());
        LOGGER.debug(replacedPermissions.toString());
        tokenEntity.getPermissions()
            .addAll(replacedPermissions);
      } ;
      LOGGER.debug(userRels.toString());
    } else {
      tokenEntity.setPermissions(request.getPermissions());
    }

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
      Optional<List<AuthType>> queryTypes, Optional<List<String>> queryPrincipals) {
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
    tokenEntity.setType(AuthType.session);
    tokenEntity.setExpirationDate(expiryDate);
    tokenEntity.setPrincipal(user.get().getId());
    List<String> permissions = new LinkedList<>();
    if (UserType.admin.equals(user.get().getType())) {
      permissions.addAll(roleRepository.findByTypeAndName("global", UserType.admin.toString()).getPermissions());
    } else if (UserType.operator.equals(user.get().getType())) {
      permissions.addAll(roleRepository.findByTypeAndName("global", UserType.operator.toString()).getPermissions());
    } else {
      // Collect all team permissions the user has
      Map<String, String> teamRefs = relationshipService.getMyTeamRefsAndRoles();
      teamRefs.forEach((k, v) -> {
        roleRepository.findByTypeAndName("team", v).getPermissions().stream().forEach(p -> permissions.add(p.replace("{principal}", k)));
      });
    }
    tokenEntity.setPermissions(permissions);
    String prefix = TokenTypePrefix.session.prefix;
    String uniqueToken = prefix + "_" + UUID.randomUUID().toString().toLowerCase();

    final String hashToken = hashString(uniqueToken);
    tokenEntity.setToken(hashToken);
    tokenEntity = tokenRepository.save(tokenEntity);

    return new Token(tokenEntity);
  }
  
  /*
   * Creates a Workflow / System token for use by the scheduled task
   */
  public Token createWorkflowSessionToken(String workflowRef) {
    CreateTokenRequest tokenRequest = new CreateTokenRequest();
    tokenRequest.setName("Scheduled Job Token");
    tokenRequest.setType(AuthType.workflow);
    tokenRequest.setPrincipal(workflowRef);
    tokenRequest.setPermissions(List.of("workflow/" + workflowRef + "/**"));
    
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.HOUR, 12);
    Date expiryDate = cal.getTime();
    tokenRequest.setExpirationDate(expiryDate);
    
    final CreateTokenResponse tokenResponse =
        this.create(tokenRequest);
    
    return this.get(tokenResponse.getToken());
  }
}
