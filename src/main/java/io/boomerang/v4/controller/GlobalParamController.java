package io.boomerang.v4.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.security.interceptors.AuthScope;
import io.boomerang.security.model.TokenAccess;
import io.boomerang.security.model.TokenObject;
import io.boomerang.security.model.TokenType;
import io.boomerang.v4.model.GlobalParam;
import io.boomerang.v4.service.GlobalParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/global-params")
@Tag(name = "Global Parameter Management",
description = "Create and Manage the Global Parameters.")
public class GlobalParamController {

  @Autowired
  private GlobalParamService paramService;

  @GetMapping(value = "")
  @AuthScope(types = {TokenType.global}, access = TokenAccess.read, object = TokenObject.parameter)
  @Operation(summary = "Get all global Params")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public List<GlobalParam> getAll() {
    return paramService.getAll();
  }

  @PostMapping(value = "/")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Create new global Param")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public GlobalParam create(@RequestBody GlobalParam request) {
    return paramService.create(request);
  }

  @PutMapping(value = "/")
  public GlobalParam update(@RequestBody GlobalParam request) {
    return paramService.update(request);
  }

  @DeleteMapping(value = "/{key}")
//  @AuthenticationScope(scopes = {TokenPermission.global})
  @Operation(summary = "Delete specific global Param")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
      @ApiResponse(responseCode = "400", description = "Bad Request")})
  public void delete(@PathVariable String key) {
    paramService.delete(key);
  }
}
