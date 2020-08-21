package net.boomerangplatform.service.crud;

import java.util.List;
import net.boomerangplatform.model.FlowSettings;


public interface ConfigurationService {

  List<FlowSettings> getAllSettings();

  List<FlowSettings> updateSettings(List<FlowSettings> settings);

}
