package net.boomerangplatform.security.model;

public class ServiceTokenUserDetails extends UserDetails {

  private final String accessToken;

  public ServiceTokenUserDetails(String email, String firstName, String lastName,
      String accessToken) {
    super(email, firstName, lastName);

    this.accessToken = accessToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

}
