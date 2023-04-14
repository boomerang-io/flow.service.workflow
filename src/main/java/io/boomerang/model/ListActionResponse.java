package io.boomerang.model;

import java.util.List;
import io.boomerang.v4.model.Action;

public class ListActionResponse {
  private Pageable pageable;

  private List<Action> records;

  public List<Action> getRecords() {
    return records;
  }


  public void setRecords(List<Action> records) {
    this.records = records;
  }


  public Pageable getPageable() {
    return pageable;
  }


  public void setPageable(Pageable pageable) {
    this.pageable = pageable;
  }
  
}
