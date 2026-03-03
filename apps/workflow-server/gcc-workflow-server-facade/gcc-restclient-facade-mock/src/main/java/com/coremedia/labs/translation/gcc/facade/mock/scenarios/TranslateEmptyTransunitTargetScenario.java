package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;

/**
 * A scenario that clears any strings to translate to provoke translation to an
 * empty string, which again may (depending on configuration) raise an error
 * {@code EMPTY_TRANSUNIT_TARGET}.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2512.0.0-1
 */
@NullMarked
public class TranslateEmptyTransunitTargetScenario implements Scenario, TranslationInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "translate-empty-transunit-target";

  @Override
  public String id() {
    return ID;
  }

  /**
   * Just returns an empty string as translation result.
   *
   * @param targetContent the translated content
   * @return an empty string
   */
  @Override
  public String postTranslate(String targetContent) {
    return "";
  }
}
