package net.boomerangplatform.security.model;

public class UserDetails {

  private String email;

  private String firstName;
  private String lastName;
  private String platformRole;

  public UserDetails(String email, String firstName, String lastName) {
    super();
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPlatformRole() {
    return platformRole;
  }

  public void setPlatformRole(String platformRole) {
    this.platformRole = platformRole;
  }
}
