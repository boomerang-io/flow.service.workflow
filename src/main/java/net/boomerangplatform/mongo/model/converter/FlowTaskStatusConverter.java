package net.boomerangplatform.mongo.model.converter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import net.boomerangplatform.mongo.model.FlowTaskStatus;

@ReadingConverter
public interface FlowTaskStatusConverter extends Converter<String, FlowTaskStatus> {

  public static List<FlowTaskStatus> convert(List<String> sources) {
    List<FlowTaskStatus> enums = new ArrayList<>();

    for (String source : sources) {
      FlowTaskStatus statusEnum = FlowTaskStatus.getFlowTaskStatus(source);
      enums.add(statusEnum);
    }
    return enums;
  }
}
