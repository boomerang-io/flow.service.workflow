package io.boomerang.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.Approval;
import io.boomerang.model.ApprovalRequest;
import io.boomerang.service.FlowApprovalService;
import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping("/workflow")
@Hidden
public class ApprovalController {

  @Autowired
  private FlowApprovalService flowApprovalService;

  @GetMapping(value = "/approvals/mine")
  public List<Approval> getApprovalsForUser() {
    return flowApprovalService.getApprovalsForUser();
  }

  @GetMapping(value = "/approvals/{teamId}")
  public List<Approval> getApprovalsForTeam(@PathVariable String teamId) {
    return flowApprovalService.getApprovalsForTeam(teamId);
  }


  @PutMapping(value = "/approvals/action")
  public void actionApproval( @RequestBody ApprovalRequest request) {
    flowApprovalService.actionApproval(request);
  }  
}
