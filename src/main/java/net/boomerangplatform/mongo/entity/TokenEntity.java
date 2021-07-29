package net.boomerangplatform.mongo.entity;

import java.util.Date;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import net.boomerangplatform.mongo.model.TokenScope;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('tokens')}")
public class TokenEntity {


  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
  public Date getExpiryDate() {
    return expiryDate;
  }
  public void setExpiryDate(Date expiryDate) {
    this.expiryDate = expiryDate;
  }
  public String getCreatorId() {
    return creatorId;
  }
  public void setCreatorId(String creatorId) {
    this.creatorId = creatorId;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public String getScopeId() {
    return scopeId;
  }
  public void setScopeId(String scopeId) {
    this.scopeId = scopeId;
  }
  public String getToken() {
    return token;
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public void setToken(String token) {
    this.token = token;
  }
  public TokenScope getScope() {
    return scope;
  }
  public void setScope(TokenScope scope) {
    this.scope = scope;
  }
  private TokenScope scope;  
  private Date creationDate;
  private Date expiryDate;
  private String creatorId;
  private String description;
  private String scopeId;
  private String token;
  @Id
  private String id;

}
