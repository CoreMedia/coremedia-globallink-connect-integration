package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;
import org.w3c.dom.Document;

/**
 * If implemented, may intercept the mock translation. Expected order of
 * processing calls:
 * <ol>
 * <li>{@link #postTranslate(String)}</li>
 * <li>{@link #postTranslate(Document)}</li>
 * </ol>
 * An implementation may choose to implement only one of the two methods.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public interface TranslationInterceptor {
  TranslationInterceptor NO_OPERATION = new TranslationInterceptor() {
  };

  /**
   * Override to modify the target content string. Applied after
   * pseudo-translation.
   *
   * @param targetContent target content to modify
   * @return target content that is the translation result
   * @implSpec Returns the {@code targetContent} unmodified
   */
  default String postTranslate(String targetContent) {
    return targetContent;
  }

  /**
   * Override to modify the XLIFF document. Applied after
   * pseudo-translation and a little after {@link #postTranslate(String)}.
   *
   * @param doc the XLIFF document to modify
   * @implSpec No operation
   */
  default void postTranslate(Document doc) {
    // No operation.
  }
}
