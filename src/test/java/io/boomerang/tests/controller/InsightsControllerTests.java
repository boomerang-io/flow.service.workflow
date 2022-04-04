package io.boomerang.tests.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.boomerang.controller.InsightsController;
import io.boomerang.misc.FlowTests;
import io.boomerang.model.InsightsSummary;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
@WithMockUser(roles = {"admin"})
@WithUserDetails("mdroy@us.ibm.com")
class InsightsControllerTests extends FlowTests {

  @Autowired
  private InsightsController insightsController;

  @Test
  void testGetInsightsTeamAndWorkflowFiltered() {
    List<String> workflowIds = new ArrayList<>();
    workflowIds.add("5d1a188af6ca2c00014c4314");

    List<String> teamIds = new ArrayList<>();
    teamIds.add("5d1a1841f6ca2c00014c4309");

    Optional<Direction> order = getOptionalOrder(Direction.ASC);
    Optional<List<String>> scopes = Optional.empty();
    Optional<String> sort = getOptionalString("sort");
    Optional<List<String>> workflowIdsList = getOptionalListString(workflowIds);
    Optional<List<String>> teamIdsList = getOptionalListString(teamIds);
    InsightsSummary summary = insightsController.getInsights(order, scopes, sort, workflowIdsList, teamIdsList,0, 2147483647, 
        Optional.empty(), Optional.empty(), Optional.empty(),Optional.empty());

    Assertions.assertEquals(5, summary.getExecutions().size());
    Long executiontime =
        (summary.getExecutions().get(0).getDuration() + summary.getExecutions().get(1).getDuration()
            + summary.getExecutions().get(2).getDuration()
            + summary.getExecutions().get(3).getDuration()
            + summary.getExecutions().get(4).getDuration()
            + summary.getExecutions().get(5).getDuration()) / summary.getExecutions().size();
    Assertions.assertEquals(executiontime, summary.getMedianExecutionTime());
    Assertions.assertEquals(5, summary.getTotalActivitiesExecuted().intValue());
  }

  @Test
  void testGetInsightsTeamFiltered() {

    List<String> teamIds = new ArrayList<>();
    teamIds.add("5d1a1841f6ca2c00014c4309");
    Optional<List<String>> scopes = Optional.empty();
    InsightsSummary summary =
        insightsController.getInsights(getOptionalOrder(Direction.ASC),
            scopes,getOptionalString("sort"), Optional.empty(), getOptionalListString(teamIds), 0,
            2147483647, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    Assertions.assertEquals(6, summary.getExecutions().size());
    Long executiontime =
        (summary.getExecutions().get(0).getDuration() + summary.getExecutions().get(1).getDuration()
            + summary.getExecutions().get(2).getDuration()
            + summary.getExecutions().get(3).getDuration()
            + summary.getExecutions().get(4).getDuration()
            + summary.getExecutions().get(5).getDuration()) / summary.getExecutions().size();
    Assertions.assertEquals(executiontime, summary.getMedianExecutionTime());
    Assertions.assertEquals(6, summary.getTotalActivitiesExecuted().intValue());
  }


  @Test
  void testGetInsights() {
    InsightsSummary summary = insightsController.getInsights(getOptionalOrder(Direction.ASC), Optional.empty(),
        getOptionalString("sort"), Optional.empty(), Optional.empty(), 0, 2147483647,
        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

    Assertions.assertEquals(6, summary.getExecutions().size());
    Long executiontime =
        (summary.getExecutions().get(0).getDuration() + summary.getExecutions().get(1).getDuration()
            + summary.getExecutions().get(2).getDuration()
            + summary.getExecutions().get(3).getDuration()
            + summary.getExecutions().get(4).getDuration()
            + summary.getExecutions().get(5).getDuration()) / summary.getExecutions().size();
    Assertions.assertEquals(executiontime, summary.getMedianExecutionTime());
    Assertions.assertEquals(6, summary.getTotalActivitiesExecuted().intValue());

  }

  Optional<String> getOptionalString(String string) {
    return Optional.of(string);
  }

  Optional<List<String>> getOptionalListString(List<String> strings) {
    return Optional.of(strings);
  }

  Optional<Direction> getOptionalOrder(Direction direction) {
    return Optional.of(direction);
  }
}