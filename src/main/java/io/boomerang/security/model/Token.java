package io.boomerang.security.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.TokenEntity;

public class Token {  
  
  private String id;
  private TokenScope type;
  private String name;
  private String description;
  private Date creationDate = new Date();
  private Date expirationDate;
  private boolean valid;
  private String principalRef;
  private List<TokenPermission> permissions = new LinkedList<>();

  public Token() {

  }

  public Token(TokenScope type) {
    super();
    this.setType(type);
  }


  public Token(TokenEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public List<TokenPermission> getPermissions() {
    return permissions;
  }
  public void setPermissions(List<TokenPermission> permissions) {
    this.permissions = permissions;
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
  public boolean isValid() {
    return valid;
  }
  public void setValid(boolean valid) {
    this.valid = valid;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  public String getPrincipalRef() {
    return principalRef;
  }

  public void setPrincipalRef(String principalRef) {
    this.principalRef = principalRef;
  }
}
