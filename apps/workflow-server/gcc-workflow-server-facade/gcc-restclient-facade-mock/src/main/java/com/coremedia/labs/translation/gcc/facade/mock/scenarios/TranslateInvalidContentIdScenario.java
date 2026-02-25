package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A scenario replaces the ID of a target content item with a string not
 * representing a valid ID format.
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2512.0.0-1
 */
@NullMarked
public class TranslateInvalidContentIdScenario implements Scenario, TranslationInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "translate-invalid-content-id";

  @Override
  public String id() {
    return ID;
  }

  /**
   * Replaces target IDs by invalid ones to simulate non-existing content items.
   *
   * @param doc the XLIFF document
   */
  @Override
  public void postTranslate(Document doc) {
    Element documentElement = doc.getDocumentElement();
    NodeList fileNodes = documentElement.getElementsByTagName("file");
    for (int i = 0; i < fileNodes.getLength(); i++) {
      String replacementId = "invalid:%d".formatted(i);
      Element fileElement = (Element) fileNodes.item(i);
      String target = fileElement.getAttributeNS("http://www.coremedia.com/2013/xliff-extensions-1.0", "target");
      if (target.startsWith("coremedia:///cap/content/")) {
        fileElement.setAttributeNS(
          "http://www.coremedia.com/2013/xliff-extensions-1.0",
          "cmxliff:target",
          replacementId
        );
      }
    }
  }
}
