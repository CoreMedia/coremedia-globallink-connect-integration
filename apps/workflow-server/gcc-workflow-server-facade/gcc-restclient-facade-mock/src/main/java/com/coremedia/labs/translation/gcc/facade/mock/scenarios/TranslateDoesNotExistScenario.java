package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.cap.common.IdHelper;
import org.jspecify.annotations.NullMarked;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A scenario replaces the ID of a target content item with a non-existing one.
 * <p>
 * As example, the content item ID {@code coremedia:///cap/content/10} in the
 * following XLIFF document will be replaced by
 * {@code coremedia:///cap/content/999998}
 * to simulate a non-existing content item.
 * {@snippet lang = xml:
 * <xliff xmlns="urn:oasis:names:tc:xliff:document:1.2" version="1.2">
 *   <file original="coremedia:///cap/version/2/1"
 *         xmlns:cmxliff="http://www.coremedia.com/2013/xliff-extensions-1.0"
 *         cmxliff:target="coremedia:///cap/content/10"
 *         source-language="en"
 *         datatype="xml"
 *         target-language="de">
 *     <!-- ... -->
 *   </file>
 * </xliff>
 *}
 * <p>
 * For all subsequent target content items, the same replacement will be applied
 * having the replacement ID decreased by two (to reference a non-existing
 * document with even ID).
 * <p>
 * <strong>Identifier:</strong> {@value #ID}
 *
 * @since 2512.0.0-1
 */
@NullMarked
public class TranslateDoesNotExistScenario implements Scenario, TranslationInterceptor {
  /**
   * The identifier of this scenario.
   */
  public static final String ID = "translate-does-not-exist";

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
    int currentReplacementId = 999998;
    for (int i = 0; i < fileNodes.getLength(); i++) {
      String replacementId = IdHelper.formatContentId(currentReplacementId);
      Element fileElement = (Element) fileNodes.item(i);
      String target = fileElement.getAttributeNS("http://www.coremedia.com/2013/xliff-extensions-1.0", "target");
      if (target.startsWith("coremedia:///cap/content/")) {
        fileElement.setAttributeNS(
          "http://www.coremedia.com/2013/xliff-extensions-1.0",
          "cmxliff:target",
          replacementId
        );
        currentReplacementId -= 2;
      }
    }
  }
}
