package io.boomerang.v4.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.mongo.model.UserStatus;
import io.boomerang.mongo.model.UserType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('users')}")
public class UserEntity {

  @Id
  private String id;
  private String email;
  private String name;
  private UserType type;
  private Boolean isFirstVisit;
  private Boolean isShowHelp;
  private Date firstLoginDate;
  private Date lastLoginDate;
  private UserStatus status;
  private Boolean hasConsented;
  private Map<String, String> labels = new HashMap<>();

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getIsFirstVisit() {
    return isFirstVisit;
  }

  public void setIsFirstVisit(Boolean isFirstVisit) {
    this.isFirstVisit = isFirstVisit;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserType getType() {
    return type;
  }

  public void setType(UserType type) {
    this.type = type;
  }

  public Date getFirstLoginDate() {
    return firstLoginDate;
  }

  public void setFirstLoginDate(Date firstLoginDate) {
    this.firstLoginDate = firstLoginDate;
  }

  public Date getLastLoginDate() {
    return lastLoginDate;
  }

  public void setLastLoginDate(Date lastLoginDate) {
    this.lastLoginDate = lastLoginDate;
  }

  public Boolean getIsShowHelp() {
    return isShowHelp;
  }

  public void setIsShowHelp(Boolean isShowHelp) {
    this.isShowHelp = isShowHelp;
  }

  public Boolean getHasConsented() {
    return hasConsented;
  }

  public void setHasConsented(Boolean hasConsented) {
    this.hasConsented = hasConsented;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
}
