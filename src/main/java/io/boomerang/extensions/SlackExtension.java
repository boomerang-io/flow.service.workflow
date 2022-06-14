package io.boomerang.extensions;

import java.util.function.Supplier;

public interface SlackExtension {

  Supplier<Boolean> createInitialRunModal(String triggerId, String userId, String workflowId);
  
}
