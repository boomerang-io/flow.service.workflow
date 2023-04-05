package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.entity.UserEntity;
import io.boomerang.v4.data.model.TeamAbstractConfiguration;
import io.boomerang.v4.model.CreateTeamRequest;
import io.boomerang.v4.model.Team;

public interface TeamService {

  ResponseEntity<Team> create(CreateTeamRequest createTeamRequest);

  ResponseEntity<Team> get(String teamId);

  Page<Team> query(int page, int limit, Sort sort, Optional<List<String>> labels,
      Optional<List<String>> status);

  List<TeamEntity> getUsersTeamListing(UserEntity userEntity);

  TeamAbstractConfiguration createParameter(String teamId, TeamAbstractConfiguration parameter);

  ResponseEntity<Void> enable(String teamId);

  ResponseEntity<Void> disable(String teamId);
}
