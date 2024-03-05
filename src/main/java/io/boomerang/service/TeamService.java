package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.data.model.Quotas;
import io.boomerang.model.Team;
import io.boomerang.model.TeamMember;
import io.boomerang.model.TeamNameCheckRequest;
import io.boomerang.model.TeamRequest;
import io.boomerang.security.model.Role;

public interface TeamService {

  ResponseEntity<?> validateName(TeamNameCheckRequest request);

  Team get(String teamId);

  Page<Team> query(Optional<Integer> queryPage, Optional<Integer> queryLimit,
      Optional<Direction> queryOrder, Optional<String> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryIds);

  Team create(TeamRequest request);

  Team patch(String teamId, TeamRequest request);

  void removeMembers(String teamId, List<TeamMember> request);

  void leave(String teamId);

  void deleteParameters(String teamId, List<String> request);

  void deleteApproverGroups(String teamId, List<String> names);

  void deleteCustomQuotas(String teamId);

  ResponseEntity<Quotas> getDefaultQuotas();

  ResponseEntity<List<Role>> getRoles();

  void delete(String team);
}
