package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.model.AbstractParam;
import io.boomerang.v4.model.ApproverGroup;
import io.boomerang.v4.model.ApproverGroupRequest;
import io.boomerang.v4.model.Team;
import io.boomerang.v4.model.TeamRequest;
import io.boomerang.v4.model.UserSummary;
import io.boomerang.v4.model.enums.TeamType;

public interface TeamService {

  ResponseEntity<Team> create(TeamRequest createTeamRequest);

  ResponseEntity<Team> create(TeamRequest request, TeamType type);

  ResponseEntity<Team> update(TeamRequest createTeamRequest);

  ResponseEntity<Team> get(String teamId);

  Page<Team> query(Optional<Integer> page, Optional<Integer> limit, Optional<Direction> sort, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> refs);

  ResponseEntity<Void> enable(String teamId);

  ResponseEntity<Void> disable(String teamId);

  ResponseEntity<List<UserSummary>> addMembers(String teamId, TeamRequest createTeamRequest);

  ResponseEntity<List<UserSummary>> removeMembers(String teamId, TeamRequest request);

  ResponseEntity<AbstractParam> createParameter(String teamId, AbstractParam parameter);
  
  ResponseEntity<Void> deleteParameter(String teamId, String key);

  ResponseEntity<List<AbstractParam>> getParameters(String teamId);
  
  ResponseEntity<AbstractParam> updateParameter(String teamId, AbstractParam parameter);

  ResponseEntity<CurrentQuotas> getQuotas(String teamId);

  ResponseEntity<Quotas> resetQuotas(String teamId);

  ResponseEntity<Quotas> patchQuotas(String teamId, Quotas quotas);

  ResponseEntity<Quotas> getDefaultQuotas();

  ResponseEntity<List<ApproverGroup>> getApproverGroups(String teamId);

  ResponseEntity<Void> deleteApproverGroup(String teamId, String name);

  ResponseEntity<ApproverGroup> createApproverGroup(String teamId,
      ApproverGroupRequest createApproverGroupRequest);

  ResponseEntity<ApproverGroup> updateApproverGroup(String teamId, ApproverGroupRequest request);

  Page<Team> mine(Optional<Integer> queryPage, Optional<Integer> queryLimit,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus);
}
