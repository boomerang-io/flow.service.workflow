package io.boomerang.security.model;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.mapping.DocumentReference;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.TokenEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.entity.ref.WorkflowEntity;

public class Token {  
  
  private String id;
  private TokenType type;
  private String name;
  private String description;
  private Date creationDate = new Date();
  private Date expirationDate;
  private boolean valid;
  @DocumentReference(lazy = true)
  private UserEntity user;
  @DocumentReference(lazy = true)
  private WorkflowEntity workflow;
  @DocumentReference(lazy = true)
  private TeamEntity team;
  private List<TokenScope> scopes = new LinkedList<>();

  public Token() {

  }

  public Token(TokenType type) {
    super();
    this.setType(type);
  }
  
  public UserEntity getUser() {
    return user;
  }

  public void setUser(UserEntity user) {
    this.user = user;
  }

  public WorkflowEntity getWorkflow() {
    return workflow;
  }

  public void setWorkflow(WorkflowEntity workflow) {
    this.workflow = workflow;
  }

  public TeamEntity getTeam() {
    return team;
  }

  public void setTeam(TeamEntity team) {
    this.team = team;
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
  public List<TokenScope> getScopes() {
    return scopes;
  }
  public void setScopes(List<TokenScope> scopes) {
    this.scopes = scopes;
  }
  public TokenType getType() {
    return type;
  }
  public void setType(TokenType type) {
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
}
