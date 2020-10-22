
package net.boomerangplatform.client.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.boomerangplatform.mongo.model.UserType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "email", "name", "isFirstVisit", "type", "isShowHelp", "firstLoginDate",
    "lastLoginDate", "lowerLevelGroupIds", "pinnedToolIds", "favoritePackages", "personalizations",
    "notificationSettings", "status", "teams", "hasConsented"})
public class UserProfile {

  @JsonProperty("id")
  private String id;
  @JsonProperty("email")
  private String email;
  @JsonProperty("name")
  private String name;
  @JsonProperty("isFirstVisit")
  private Boolean isFirstVisit;
  @JsonProperty("type")
  private UserType type;
  @JsonProperty("isShowHelp")
  private Boolean isShowHelp;
  @JsonProperty("firstLoginDate")
  private String firstLoginDate;
  @JsonProperty("lastLoginDate")
  private String lastLoginDate;
  @JsonProperty("lowerLevelGroupIds")
  private List<LowerLevelGroup> lowerLevelGroups = null;
  @JsonProperty("pinnedToolIds")
  private List<Object> pinnedToolIds = null;
  @JsonProperty("favoritePackages")
  private List<Object> favoritePackages = null;

  @JsonProperty("status")
  private String status;
  @JsonProperty("teams")
  private List<Team> teams = null;
  @JsonProperty("hasConsented")
  private Boolean hasConsented;

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(String email) {
    this.email = email;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("isFirstVisit")
  public Boolean getIsFirstVisit() {
    return isFirstVisit;
  }

  @JsonProperty("isFirstVisit")
  public void setIsFirstVisit(Boolean isFirstVisit) {
    this.isFirstVisit = isFirstVisit;
  }

  @JsonProperty("type")
  public UserType getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(UserType type) {
    this.type = type;
  }

  @JsonProperty("isShowHelp")
  public Boolean getIsShowHelp() {
    return isShowHelp;
  }

  @JsonProperty("isShowHelp")
  public void setIsShowHelp(Boolean isShowHelp) {
    this.isShowHelp = isShowHelp;
  }

  @JsonProperty("firstLoginDate")
  public String getFirstLoginDate() {
    return firstLoginDate;
  }

  @JsonProperty("firstLoginDate")
  public void setFirstLoginDate(String firstLoginDate) {
    this.firstLoginDate = firstLoginDate;
  }

  @JsonProperty("lastLoginDate")
  public String getLastLoginDate() {
    return lastLoginDate;
  }

  @JsonProperty("lastLoginDate")
  public void setLastLoginDate(String lastLoginDate) {
    this.lastLoginDate = lastLoginDate;
  }

  @JsonProperty("lowerLevelGroups")
  public List<LowerLevelGroup> getLowerLevelGroups() {
    return lowerLevelGroups;
  }

  @JsonProperty("lowerLevelGroups")
  public void setLowerLevelGroups(List<LowerLevelGroup> lowerLevelGroups) {
    this.lowerLevelGroups = lowerLevelGroups;
  }

  @JsonProperty("pinnedToolIds")
  public List<Object> getPinnedToolIds() {
    return pinnedToolIds;
  }

  @JsonProperty("pinnedToolIds")
  public void setPinnedToolIds(List<Object> pinnedToolIds) {
    this.pinnedToolIds = pinnedToolIds;
  }

  @JsonProperty("favoritePackages")
  public List<Object> getFavoritePackages() {
    return favoritePackages;
  }

  @JsonProperty("favoritePackages")
  public void setFavoritePackages(List<Object> favoritePackages) {
    this.favoritePackages = favoritePackages;
  }

  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(String status) {
    this.status = status;
  }

  @JsonProperty("teams")
  public List<Team> getTeams() {
    return teams;
  }

  @JsonProperty("teams")
  public void setTeams(List<Team> teams) {
    this.teams = teams;
  }

  @JsonProperty("hasConsented")
  public Boolean getHasConsented() {
    return hasConsented;
  }

  @JsonProperty("hasConsented")
  public void setHasConsented(Boolean hasConsented) {
    this.hasConsented = hasConsented;
  }
}
