package net.boomerangplatform.model;

import net.boomerangplatform.mongo.model.WorkflowQuotas;

public class QuotasResponse {

  private WorkflowQuotas quotas;

  public WorkflowQuotas getQuotas() {
    return quotas;
  }

  public void setQuotas(WorkflowQuotas quotas) {
    this.quotas = quotas;
  }
  
}
