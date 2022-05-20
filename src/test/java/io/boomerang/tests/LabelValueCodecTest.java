package io.boomerang.tests;


import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.boomerang.util.LabelValueCodec;

@ExtendWith(SpringExtension.class)
public class LabelValueCodecTest {

  @Test
  public void testEncode() {
    String testString = "This is a test string! #1 $% :)";
    Assertions.assertTrue(Strings.isNotBlank(LabelValueCodec.encode(testString)));
  }

  @Test
  public void testDecode() {
    String testEncodedString = "VGhpcyBpcyBhIHRlc3Qgc3RyaW5nISAjMSAkJSA6KQ--";
    Assertions.assertTrue(Strings.isNotBlank(LabelValueCodec.decode(testEncodedString)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"key", "a value", "some text, with comma", "yes!!!",
      "{\"just_a_string\":\"It did go through!\",\"just_a_num\":69420}", "[0,1,2,3,4,5,6]", "42069",
      "\"A string!!!\"", "false", "[\"this\",false,202]"})
  public void testEncodeDecode(String testString) {
    String encodedString = LabelValueCodec.encode(testString);
    String decodedString = LabelValueCodec.decode(encodedString);
    Assertions.assertEquals(testString, decodedString);
  }
}
