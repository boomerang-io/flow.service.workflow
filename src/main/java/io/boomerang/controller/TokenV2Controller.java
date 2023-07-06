package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.CreateTokenRequest;
import io.boomerang.security.model.CreateTokenResponse;
import io.boomerang.security.model.Token;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenScope;
import io.boomerang.security.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2")
@Tag(name = "Token Management",
description = "Create and retrieve Tokens")
public class TokenV2Controller {

  @Autowired
  private TokenService tokenService;

  @PostMapping("/token")
  @AuthScope(types = {TokenScope.global, TokenScope.user, TokenScope.team, TokenScope.workflow},
      object = TokenObject.token, access = TokenAccess.write)
  @Operation(summary = "Create Token")
  public CreateTokenResponse createToken(@Valid @RequestBody CreateTokenRequest request) {
    return tokenService.create(request);
  }

  @GetMapping("/token/query")
  @AuthScope(types = {TokenScope.global, TokenScope.user, TokenScope.team, TokenScope.workflow},
      object = TokenObject.token, access = TokenAccess.read)
  @Operation(summary = "Search for Tokens")
  public Page<Token> query(
      @Parameter(name = "types", description = "List of types to filter for. Defaults to all.",
      required = false) @RequestParam(required = false)  Optional<List<TokenScope>> types,
      @Parameter(name = "principals", description = "List of principals to filter for. Based on the types you are querying for.",
      required = false) @RequestParam(required = false)  Optional<List<String>> principals,
      @Parameter(name = "limit", description = "Result Size", example = "10",
      required = true) @RequestParam(required = false) Optional<Integer> limit,
  @Parameter(name = "page", description = "Page Number", example = "0",
      required = true) @RequestParam(defaultValue = "0") Optional<Integer> page,
  @Parameter(name = "order", description = "Ascending (ASC) or Descending (DESC) sort order on creationDate", example = "ASC",
  required = true) @RequestParam(defaultValue = "ASC") Optional<Direction> order,
      @Parameter(name = "sort", description = "The element to sort onr", example = "0",
      required = false) @RequestParam(defaultValue = "creationDate") Optional<String> sort,
      @Parameter(name = "fromDate", description = "The unix timestamp / date to search from in milliseconds since epoch", example = "1677589200000",
      required = false) @RequestParam Optional<Long> fromDate,
      @Parameter(name = "toDate", description = "The unix timestamp / date to search to in milliseconds since epoch", example = "1680267600000",
      required = false) @RequestParam Optional<Long> toDate) {
    Optional<Date> from = Optional.empty();
    Optional<Date> to = Optional.empty();
    if (fromDate.isPresent()) {
      from = Optional.of(new Date(fromDate.get()));
    }
    if (toDate.isPresent()) {
      to = Optional.of(new Date(toDate.get()));
    }
    return tokenService.query(from, to, limit, page, order, sort, types, principals);
  }

  @DeleteMapping("/token/{id}")
  @AuthScope(types = {TokenScope.global, TokenScope.user, TokenScope.team, TokenScope.workflow},
      object = TokenObject.token, access = TokenAccess.delete)
  @Operation(summary = "Delete Token")
  public ResponseEntity<?> deleteToken(@Valid @PathParam(value = "id") String id) {
    boolean result = tokenService.delete(id);
    if (result) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.badRequest().build();
    }
  }
}
