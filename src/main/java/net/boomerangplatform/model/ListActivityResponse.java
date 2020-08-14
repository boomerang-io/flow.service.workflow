package net.boomerangplatform.model;

import java.util.List;

public class ListActivityResponse {

  private Pageable pageable;

  private List<FlowActivity> records;

  public List<FlowActivity> getRecords() {
    return records;
  }


  public void setRecords(List<FlowActivity> records) {
    this.records = records;
  }


  public Pageable getPageable() {
    return pageable;
  }


  public void setPageable(Pageable pageable) {
    this.pageable = pageable;
  }
  
  

}
