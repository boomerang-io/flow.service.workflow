package io.boomerang.service.crud;

import java.util.List;
import io.boomerang.model.FlowSettings;


public interface ConfigurationService {

  List<FlowSettings> getAllSettings();

  List<FlowSettings> updateSettings(List<FlowSettings> settings);

}
