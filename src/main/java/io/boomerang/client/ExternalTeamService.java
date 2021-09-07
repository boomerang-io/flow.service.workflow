package io.boomerang.client;

import java.util.List;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TeamEntity;

public interface ExternalTeamService {

  List<TeamEntity>  getExternalTeams(String url);
  List<FlowUserEntity> getExternalTeamMemberListing(String teamId);
}
