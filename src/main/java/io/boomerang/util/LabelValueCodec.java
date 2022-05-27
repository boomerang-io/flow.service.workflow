package io.boomerang.util;

import java.util.Base64;

public final class LabelValueCodec {

  public static String encode(String rawValue) {
    String encodedValue = Base64.getEncoder().encodeToString(rawValue.getBytes());
    return encodedValue.replace('=', '-').concat("x");
  }

  public static String decode(String encodedValue) {
    String unmangledString = encodedValue.substring(0, encodedValue.length() - 1).replace('-', '=');
    return new String(Base64.getDecoder().decode(unmangledString));
  }
}
