package io.boomerang.client;

import java.util.List;
import io.boomerang.mongo.entity.FlowTeamEntity;

public interface ExternalTeamService {

  List<FlowTeamEntity>  getExternalTeams(String url);
}
