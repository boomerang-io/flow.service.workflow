package io.boomerang.extensions;

import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class SlackExtensionImpl implements SlackExtension {

  private static final Logger LOGGER = LogManager.getLogger();
  
  public Supplier<Boolean> createModal(String triggerId) {
    return () -> {
      LOGGER.debug("Trigger ID:" + triggerId);
      return true;
      };
  }
}
