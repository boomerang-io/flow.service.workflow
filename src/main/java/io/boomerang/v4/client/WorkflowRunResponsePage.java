package io.boomerang.v4.client;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.v4.data.entity.ref.WorkflowRunEntity;

public class WorkflowRunResponsePage extends PageImpl<WorkflowRunEntity> {
  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public WorkflowRunResponsePage(@JsonProperty("content") List<WorkflowRunEntity> content,
                      @JsonProperty("number") int number,
                      @JsonProperty("size") int size,
                      @JsonProperty("totalElements") Long totalElements,
                      @JsonProperty("pageable") JsonNode pageable,
                      @JsonProperty("last") boolean last,
                      @JsonProperty("totalPages") int totalPages,
                      @JsonProperty("sort") JsonNode sort,
                      @JsonProperty("first") boolean first,
                      @JsonProperty("numberOfElements") int numberOfElements) {

      super(content, PageRequest.of(number, size), totalElements);
  }

  public WorkflowRunResponsePage(List<WorkflowRunEntity> content, Pageable pageable, long total) {
      super(content, pageable, total);
  }

  public WorkflowRunResponsePage(List<WorkflowRunEntity> content) {
      super(content);
  }

  public WorkflowRunResponsePage() {
      super(new ArrayList<>());
  }
}
