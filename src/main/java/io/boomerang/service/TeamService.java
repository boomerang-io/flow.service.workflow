package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.security.model.Role;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.model.ApproverGroup;
import io.boomerang.v4.model.ApproverGroupRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.TeamMember;
import io.boomerang.v4.model.TeamNameCheckRequest;
import io.boomerang.v4.model.TeamRequest;
import io.boomerang.v4.model.enums.TeamType;

public interface TeamService {

  ResponseEntity<?> validateName(TeamNameCheckRequest request);

  Team get(String teamId);

  Page<Team> query(Optional<Integer> queryPage, Optional<Integer> queryLimit,
      Optional<Direction> queryOrder, Optional<String> querySort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryIds);

  Team create(TeamRequest request, TeamType type);

  Team patch(String teamId, TeamRequest request);

  void removeMembers(String teamId, List<TeamMember> request);

  void deleteParameters(String teamId, List<String> request);

  ResponseEntity<CurrentQuotas> getQuotas(String teamId);

  ResponseEntity<Quotas> resetQuotas(String teamId);

  ResponseEntity<Quotas> patchQuotas(String teamId, Quotas quotas);

  ResponseEntity<Quotas> getDefaultQuotas();

  List<ApproverGroup> getApproverGroups(String teamId);

  void deleteApproverGroup(String teamId, String name);

  ApproverGroup createApproverGroup(String teamId,
      ApproverGroupRequest createApproverGroupRequest);

  ApproverGroup updateApproverGroup(String teamId, ApproverGroupRequest request);

  ResponseEntity<List<Role>> getRoles();
}
