package net.boomerangplatform.mongo.model.converter;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;

@ReadingConverter
public abstract class FlowTriggerEnumConverter implements Converter<String, FlowTriggerEnum> { // NOSONAR

  private FlowTriggerEnumConverter() {
    // Do nothing
  }

  public static List<FlowTriggerEnum> convert(List<String> sources) {
    List<FlowTriggerEnum> enums = new ArrayList<>();

    for (String source : sources) {
      FlowTriggerEnum triggerEnum = FlowTriggerEnum.getFlowTriggerEnum(source);
      enums.add(triggerEnum);
    }
    return enums;
  }
}
