package net.boomerangplatform.model;

import java.util.List;
import org.springframework.data.domain.Page;
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;

public class ListActivityResponse {

  private Page<FlowWorkflowActivityEntity> pageable;

  private List<FlowActivity> records;

  public Page<FlowWorkflowActivityEntity> getPageable() {
    return pageable;
  }

  public List<FlowActivity> getRecords() {
    return records;
  }

  public void setPageable(Page<FlowWorkflowActivityEntity> pageable) {
    this.pageable = pageable;
  }

  public void setRecords(List<FlowActivity> records) {
    this.records = records;
  }

}
