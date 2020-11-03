package net.boomerangplatform.client;

import java.util.List;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;

public interface BoomerangCICDService {

  List<FlowTeamEntity>  getCICDTeams();
}
