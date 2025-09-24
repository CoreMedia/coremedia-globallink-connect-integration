package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;
import org.w3c.dom.Document;

/**
 * A scenario that introduces an invalid element into the XLIFF document.
 * <p>
 * This can be used to test how well the system handles invalid XLIFF files.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class TranslateInvalidXliffScenario implements Scenario, TranslationInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "translate-invalid-xliff";

  @Override
  public String id() {
    return ID;
  }

  /**
   * Introduces an invalid element into the XLIFF document.
   *
   * @param doc the XLIFF document
   */
  @Override
  public void postTranslate(Document doc) {
    doc.getDocumentElement().appendChild(doc.createElementNS("intentionally", "invalid"));
  }
}
