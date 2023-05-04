package io.boomerang.v4.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import io.boomerang.model.UserQueryResult;
import io.boomerang.model.profile.SortSummary;
import io.boomerang.security.service.IdentityService;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "List, Create, update and delete Users.")
public class UsersV2Controller {

  @Value("${flow.externalUrl.user}")
  private String flowExternalUrlUser;

  @Autowired
  private IdentityService userIdentityService;

  @GetMapping(value = "/{userId}")
//  @AuthScope(scopes = {TokenPermission.global})
  @Operation(summary = "Get a users details")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "404", description = "Not Found")})
  public ResponseEntity<User> getUser(@PathVariable String userId) {
    UserEntity flowUser = userIdentityService.getUserByID(userId);
    if (userIdentityService.getUserByID(userId) != null) {
      User user = new User();
      BeanUtils.copyProperties(flowUser, user);
      return ResponseEntity.ok(user);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @PatchMapping(value = "/users/{userId}")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Update a Boomerang Flow user details")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> updateFlowUser(@PathVariable String userId,
      @RequestBody User flowUser) {
    if (isUserManagementAvaliable()) {
      userIdentityService.updateFlowUser(userId, flowUser);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping(value = "/users/{userId}")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Delete a Boomerang Flow user")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<Void> deleteFlowUser(@PathVariable String userId) {
    if (isUserManagementAvaliable()) {
      userIdentityService.deleteFlowUser(userId);
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @PostMapping(value = "/users")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Create a new Boomerang Flow user")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<User> addUser(@RequestBody User flowUser) {
    if (isUserManagementAvaliable()) {
      User flowUserEntity = userIdentityService.addFlowUser(flowUser);
      return ResponseEntity.ok(flowUserEntity);
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping(value = "/users")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Search for users registed on Boomerang Flow")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public ResponseEntity<UserQueryResult> getUsers(@RequestParam(required = false) String query,
      @RequestParam(defaultValue = "ASC") Direction order,
      @RequestParam(required = false) String sort, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {

    if (isUserManagementAvaliable()) {
      Sort pagingSort = Sort.by(Direction.ASC, "firstLoginDate");

      SortSummary sortSummary = new SortSummary();
      sortSummary.setProperty("firstLoginDate");
      sortSummary.setDirection(Direction.ASC.toString());

      if (StringUtils.isNotBlank(sort)) {
        Direction direction = order == null ? Direction.ASC : order;
        sortSummary.setDirection(direction.toString());
        sortSummary.setProperty(sort);
        pagingSort = Sort.by(direction, sort);
      }

      final Pageable pageable = PageRequest.of(page, size, pagingSort);
      if (StringUtils.isNotBlank(query)) {
        UserQueryResult result = userIdentityService.getUserViaSearchTerm(query, pageable);
        result.setupSortSummary(sortSummary);
        return ResponseEntity.ok(result);
      } else {
        UserQueryResult result = userIdentityService.getUsers(pageable);
        result.setupSortSummary(sortSummary);
        return ResponseEntity.ok(result);
      }
    } else {
      return ResponseEntity.badRequest().build();
    }
  }

  private boolean isUserManagementAvaliable() {
    return flowExternalUrlUser.isBlank();
  }
}
