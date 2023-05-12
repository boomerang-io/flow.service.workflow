package io.boomerang.v4.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenScope;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.model.User;
import io.boomerang.v4.model.UserProfile;
import io.boomerang.v4.model.UserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/user")
@Tag(name = "User Management", description = "List, Create, update and delete Users.")
public class UserV2Controller {

  @Value("${flow.externalUrl.user}")
  private String flowExternalUrlUser;

  @Autowired
  private IdentityService identityService;

  /* 
   * Returns the current users profile
   * 
   * The authentication handler ensures they are already a registered user
   */
  @GetMapping(value = "/profile")
  @AuthScope(access = TokenAccess.read, object = TokenObject.user, types = {TokenScope.session, TokenScope.user})
  @Operation(summary = "Get your Profile")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "423", description = "OK"),
      @ApiResponse(responseCode = "404", description = "Instance not activated. Profile locked.")})
  public UserProfile getProfile() {
    return identityService.getCurrentProfile();
  }

  @GetMapping(value = "/{userId}")
  @AuthScope(access = TokenAccess.read, object = TokenObject.user, types = {TokenScope.session, TokenScope.user, TokenScope.team, TokenScope.global})
  @Operation(summary = "Get a Users details")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "404", description = "Not Found")})
  public ResponseEntity<User> getUserByID(@PathVariable String userId) {
    Optional<User> user = identityService.getUserByID(userId);
    if (user.isPresent()) {
      return ResponseEntity.ok(user.get());
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping(value = "/query")
  @AuthScope(access = TokenAccess.read, object = TokenObject.user, types = {TokenScope.session, TokenScope.user, TokenScope.team, TokenScope.global})
  @Operation(summary = "Search for Users")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public Page<User> getUsers(@Parameter(name = "labels",
      description = "List of url encoded labels. For example Organization=Boomerang,customKey=test would be encoded as Organization%3DBoomerang,customKey%3Dtest)",
      required = false) @RequestParam(required = false) Optional<List<String>> labels,
      @Parameter(name = "status", description = "List of statuses to filter for. Defaults to all.",
          example = "active,inactive",
          required = false) @RequestParam(required = false) Optional<List<String>> status,
      @Parameter(name = "ids", description = "List of ids to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> ids,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "sort", description = "Ascending (ASC) or Descending (DESC) sort on creationDate", example = "ASC",
  required = true) @RequestParam(defaultValue = "ASC") Optional<Direction> sort) {
    return identityService.query(page, limit, sort, labels, status, ids);
  }

  @PostMapping(value = "/")
  @AuthScope(access = TokenAccess.write, object = TokenObject.user, types = {TokenScope.global})
  @Operation(summary = "Create a new Boomerang Flow user")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<User> addUser(@RequestBody UserRequest request) {
    if (isUserManagementAvaliable()) {
      User flowUserEntity = identityService.create(request);
      return ResponseEntity.ok(flowUserEntity);
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @PatchMapping(value = "/")
  @AuthScope(access = TokenAccess.write, object = TokenObject.user, types = {TokenScope.global})
  @Operation(summary = "Update a Boomerang Flow Users details")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> apply(@RequestBody UserRequest user) {
    if (isUserManagementAvaliable()) {
      identityService.apply(user);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping(value = "/{userId}")
  @AuthScope(access = TokenAccess.delete, object = TokenObject.user, types = {TokenScope.global})
  @Operation(summary = "Delete a Boomerang Flow user")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> deleteFlowUser(@PathVariable String userId) {
    if (isUserManagementAvaliable()) {
      identityService.delete(userId);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  private boolean isUserManagementAvaliable() {
    return flowExternalUrlUser.isBlank();
  }
}
