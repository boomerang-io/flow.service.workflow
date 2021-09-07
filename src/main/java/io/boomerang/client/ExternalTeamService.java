package io.boomerang.client;

import java.util.List;
import io.boomerang.model.FlowUser;
import io.boomerang.mongo.entity.TeamEntity;

public interface ExternalTeamService {

  List<TeamEntity>  getExternalTeams(String url);
  List<FlowUser> getExternalTeamMemberListing(String teamId);
}
