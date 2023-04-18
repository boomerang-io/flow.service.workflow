package io.boomerang.mongo.model.converter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import io.boomerang.v4.model.enums.TriggerEnum;

@ReadingConverter
public abstract class FlowTriggerEnumConverter implements Converter<String, TriggerEnum> { // NOSONAR

  private FlowTriggerEnumConverter() {
    // Do nothing
  }

  public static List<TriggerEnum> convert(List<String> sources) {
    List<TriggerEnum> enums = new ArrayList<>();

    for (String source : sources) {
      TriggerEnum triggerEnum = TriggerEnum.getFlowTriggerEnum(source);
      enums.add(triggerEnum);
    }
    return enums;
  }
}
