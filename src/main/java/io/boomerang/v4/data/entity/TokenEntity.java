package io.boomerang.v4.data.entity;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.security.model.TokenPermission;
import io.boomerang.security.model.TokenScope;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('tokens')}")
public class TokenEntity {

  @Id
  private String id;
  private TokenScope type;
  private String name;
  private String description;
  private Date creationDate = new Date();
  private Date expirationDate;
  private String principal;
  private List<TokenPermission> permissions = new LinkedList<>();
  private String token;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TokenScope getType() {
    return type;
  }

  public void setType(TokenScope type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getExpirationDate() {
    return expirationDate;
  }

  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public List<TokenPermission> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<TokenPermission> permissions) {
    this.permissions = permissions;
  }

  public String getPrincipal() {
    return principal;
  }

  public void setPrincipal(String principal) {
    this.principal = principal;
  }
}