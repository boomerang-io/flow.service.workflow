package io.boomerang.service.crud;

import java.util.List;
import org.springframework.data.domain.Pageable;
import io.boomerang.model.FlowTeam;
import io.boomerang.model.TeamMember;
import io.boomerang.model.TeamQueryResult;
import io.boomerang.model.TeamWorkflowSummary;
import io.boomerang.model.WorkflowQuotas;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.mongo.entity.FlowTeamConfiguration;
import io.boomerang.mongo.entity.TeamEntity;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.model.Quotas;

public interface TeamService {

  FlowTeam createStandaloneTeam(String name);

  TeamQueryResult getAllAdminTeams(Pageable pageable);

  List<TeamWorkflowSummary> getTeamListing(FlowUserEntity userEntity);
  
  List<TeamWorkflowSummary> getAllTeamListing();

  List<TeamWorkflowSummary> getAllTeams();

  void createFlowTeam(String higherLevelGroupId, String teamName);


  List<TeamWorkflowSummary> getUserTeams(FlowUserEntity userEntity);

  List<FlowTeamConfiguration> getAllTeamProperties(String teamId);

  void deleteTeamProperty(String teamId, String configurationId);

  List<FlowTeamConfiguration> updateTeamProperty(String teamId, FlowTeamConfiguration property);

  FlowTeamConfiguration createNewTeamProperty(String teamId, FlowTeamConfiguration property);

  void updateTeamMembers(String teamId, List<String> teamMembers);

  FlowTeam getTeamById(String teamId);
  
  FlowTeam getTeamByIdDetailed(String teamId);


  void updateTeam(String teamId, FlowTeam flow);

  WorkflowQuotas getTeamQuotas(String teamId);

  WorkflowQuotas resetTeamQuotas(String teamId);

  Quotas updateTeamQuotas(String teamId, Quotas quotas);

  Quotas updateQuotasForTeam(String teamId, Quotas quotas);

  Quotas getDefaultQuotas();

  TeamEntity deactivateTeam(String teamId);

  void updateSummaryWithUpgradeFlags(List<WorkflowSummary> workflowSummary);

  List<TeamEntity> getUsersTeamListing(FlowUserEntity userEntity);

  List<TeamMember> getTeamMembers(String teamId);
  

}
