package net.boomerangplatform.model.profile;

import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"totalPages", "totalElements", "last", "sort", "first", "numberOfElements",
    "size", "number"})
public class PageableSummary {

  private Integer totalPages;
  private Long totalElements;
  private Boolean last;
  private List<SortSummary> sort;
  private Boolean first;
  private Integer numberOfElements;
  private Integer size;
  private Integer number;

  @JsonProperty("totalPages")
  public Integer getTotalPages() {
    return totalPages;
  }

  @JsonProperty("totalPages")
  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  @JsonProperty("totalElements")
  public Long getTotalElements() {
    return totalElements;
  }

  @JsonProperty("totalElements")
  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  @JsonProperty("last")
  public Boolean getLast() {
    return last;
  }

  @JsonProperty("last")
  public void setLast(Boolean last) {
    this.last = last;
  }

  @JsonProperty("sort")
  public List<SortSummary> getSort() {
    return sort;
  }

  @JsonProperty("sort")
  public void setSort(List<SortSummary> sort) {
    this.sort = sort;
  }

  @JsonProperty("first")
  public Boolean getFirst() {
    return first;
  }

  @JsonProperty("first")
  public void setFirst(Boolean first) {
    this.first = first;
  }

  @JsonProperty("numberOfElements")
  public Integer getNumberOfElements() {
    return numberOfElements;
  }

  @JsonProperty("numberOfElements")
  public void setNumberOfElements(Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
  }

  @JsonProperty("size")
  public Integer getSize() {
    return size;
  }

  @JsonProperty("size")
  public void setSize(Integer size) {
    this.size = size;
  }

  @JsonProperty("number")
  public Integer getNumber() {
    return number;
  }

  @JsonProperty("number")
  public void setNumber(Integer number) {
    this.number = number;
  }

  @JsonIgnore
  public void setPageable(Page<?> pages) {
    BeanUtils.copyProperties(pages, this);
    this.totalElements = pages.getTotalElements();
  }

  @JsonIgnore
  public void setupSortSummary(SortSummary summary) {

    if ("ASC".equals(summary.getDirection())) {
      summary.setAscending(true);
    } else if ("DESC".equals(summary.getDirection())) {
      summary.setDescending(true);
    }
    List<SortSummary> sortSummaryList = new LinkedList<>();
    sortSummaryList.add(summary);
    this.sort = sortSummaryList;
  }

}
