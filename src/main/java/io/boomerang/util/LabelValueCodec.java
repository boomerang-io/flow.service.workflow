package io.boomerang.util;

import java.util.Base64;

public final class LabelValueCodec {

  public static String encode(String rawValue) {
    String encodedValue = Base64.getEncoder().encodeToString(rawValue.getBytes());
    return encodedValue.replace('=', '-');
  }

  public static String decode(String encodedValue) {
    return new String(Base64.getDecoder().decode(encodedValue.replace('-', '=')));
  }
}
