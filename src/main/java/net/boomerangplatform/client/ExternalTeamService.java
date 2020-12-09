package net.boomerangplatform.client;

import java.util.List;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;

public interface ExternalTeamService {

  List<FlowTeamEntity>  getExternalTeams(String url);
}
