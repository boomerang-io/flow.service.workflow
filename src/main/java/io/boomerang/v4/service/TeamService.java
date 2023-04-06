package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.data.model.Quotas;
import io.boomerang.v4.data.model.TeamParameter;
import io.boomerang.v4.model.ApproverGroup;
import io.boomerang.v4.model.ApproverGroupCreateRequest;
import io.boomerang.v4.model.CreateTeamRequest;
import io.boomerang.v4.model.Team;

public interface TeamService {

  ResponseEntity<Team> create(CreateTeamRequest createTeamRequest);

  ResponseEntity<Team> get(String teamId);

  Page<Team> query(int page, int limit, Sort sort, Optional<List<String>> labels,
      Optional<List<String>> status);

  ResponseEntity<Void> enable(String teamId);

  ResponseEntity<Void> disable(String teamId);

  ResponseEntity<TeamParameter> createParameter(String teamId, TeamParameter parameter);
  
  ResponseEntity<Void> deleteParameter(String teamId, String key);

  ResponseEntity<List<TeamParameter>> getParameters(String teamId);
  
  ResponseEntity<TeamParameter> updateParameter(String teamId, TeamParameter parameter);

  ResponseEntity<CurrentQuotas> getQuotas(String teamId);

  ResponseEntity<Quotas> resetQuotas(String teamId);

  ResponseEntity<Quotas> patchQuotas(String teamId, Quotas quotas);

  ResponseEntity<Quotas> getDefaultQuotas();

  ResponseEntity<List<ApproverGroup>> getApproverGroups(String teamId);

  ResponseEntity<Void> deleteApproverGroup(String teamId, String name);

  ResponseEntity<ApproverGroup> createApproverGroup(String teamId,
      ApproverGroupCreateRequest createApproverGroupRequest);

  ResponseEntity<Team> updateTeam(CreateTeamRequest createTeamRequest);
}