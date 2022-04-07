package io.boomerang.mongo.model.converter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import io.boomerang.mongo.model.TaskStatus;

@ReadingConverter
public interface FlowTaskStatusConverter extends Converter<String, TaskStatus> {

  public static List<TaskStatus> convert(List<String> sources) {
    List<TaskStatus> enums = new ArrayList<>();

    for (String source : sources) {
      TaskStatus statusEnum = TaskStatus.getFlowTaskStatus(source);
      enums.add(statusEnum);
    }
    return enums;
  }
}
