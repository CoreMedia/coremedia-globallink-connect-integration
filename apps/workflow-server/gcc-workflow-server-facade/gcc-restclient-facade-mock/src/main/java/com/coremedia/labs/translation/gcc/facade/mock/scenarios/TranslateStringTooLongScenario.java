package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;

/**
 * A scenario that appends some Lorem Ipsum text to translated strings
 * to reach a length of at least {@value #LENGTH_CHALLENGE} characters.
 * <p>
 * This can be used to test how well the system handles very long strings,
 * which may be a challenge for some properties in the content-type-model.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class TranslateStringTooLongScenario implements Scenario, TranslationInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "translate-string-too-long";
  /**
   * A string length, which we consider a challenge at least for some
   * string properties in the content-type-model.
   * <p>
   * Note, that this is applied to any string, even rich text, as this strategy
   * is blind regarding the actual property affected.
   */
  private static final int LENGTH_CHALLENGE = 2048;

  @Override
  public String id() {
    return ID;
  }

  /**
   * Appends some Lorem Ipsum text to the target content to reach at least
   * {@value #LENGTH_CHALLENGE} characters.
   *
   * @param targetContent the translated content
   * @return the translated content, possibly extended with Lorem Ipsum text
   */
  @Override
  public String postTranslate(String targetContent) {
    StringBuilder builder = new StringBuilder(targetContent);
    builder.append(LoremIpsum.loremIpsum(Math.max(LENGTH_CHALLENGE - builder.length(), 0)));
    return builder.toString();
  }
}
