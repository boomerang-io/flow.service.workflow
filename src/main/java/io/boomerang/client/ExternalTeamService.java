package io.boomerang.client;

import java.util.List;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;

public interface ExternalTeamService {

  List<TeamEntity>  getExternalTeams(String url);
  List<UserEntity> getExternalTeamMemberListing(String teamId);
}
