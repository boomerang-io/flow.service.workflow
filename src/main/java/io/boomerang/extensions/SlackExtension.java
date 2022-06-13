package io.boomerang.extensions;

import java.util.function.Supplier;

public interface SlackExtension {

  Supplier<Boolean> createModal(String string);
  
}
