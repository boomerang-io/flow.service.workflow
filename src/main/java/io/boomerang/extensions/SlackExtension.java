package io.boomerang.extensions;

import java.util.function.Supplier;

public interface SlackExtension {

  Supplier<Boolean> createInitialModal(String triggerId, String workflowId);
  
}
