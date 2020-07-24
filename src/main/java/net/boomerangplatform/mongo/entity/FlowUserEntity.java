package net.boomerangplatform.mongo.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import net.boomerangplatform.mongo.model.UserStatus;
import net.boomerangplatform.mongo.model.UserType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "flow_users")
public class FlowUserEntity {

  @Id
  private String id;

  private String email;
  private String name;
  private Boolean isFirstVisit;
  private UserType type;
  private Boolean isShowHelp;
  private Date firstLoginDate;
  private Date lastLoginDate;

  private List<String> flowTeams;

  private UserStatus status;

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

  public List<String> getFlowTeams() {
    return flowTeams;
  }

  public void setFlowTeams(List<String> flowTeams) {
    this.flowTeams = flowTeams;
  }
}
