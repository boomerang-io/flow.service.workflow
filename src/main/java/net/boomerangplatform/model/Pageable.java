package net.boomerangplatform.model;

import java.util.List;

public class Pageable {

  private Integer number;
  private Integer size;
  private List<Sort> sort;
  private Long totalElements;
  private boolean first;
  private boolean last;
  private Integer totalPages;
  private Integer numberOfElements;

  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public List<Sort> getSort() {
    return sort;
  }

  public void setSort(List<Sort> sort) {
    this.sort = sort;
  }

  public Long getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  public boolean isFirst() {
    return first;
  }

  public void setFirst(boolean first) {
    this.first = first;
  }

  public boolean isLast() {
    return last;
  }

  public void setLast(boolean last) {
    this.last = last;
  }

  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  public Integer getNumberOfElements() {
    return numberOfElements;
  }

  public void setNumberOfElements(Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
  }



}
