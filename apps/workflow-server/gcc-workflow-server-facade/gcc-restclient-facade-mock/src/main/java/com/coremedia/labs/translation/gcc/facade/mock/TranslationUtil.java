package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.mock.scenarios.TranslationInterceptor;
import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * Provides simple <em>mock</em> translation by replacing characters.
 */
@NullMarked
final class TranslationUtil {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Map<String, String> TRANSLATE = ImmutableMap.<String, String>builder()
    .put("a", "â")
    .put("b", "ƀ")
    .put("c", "ƈ")
    .put("d", "ď")
    .put("e", "é")
    .put("f", "ḟ")
    .put("g", "ġ")
    .put("h", "ĥ")
    .put("i", "ĩ")
    .put("j", "ɉ")
    .put("k", "ķ")
    .put("l", "ļ")
    .put("m", "Ɯ")
    .put("n", "ň")
    .put("o", "ō")
    .put("p", "ƥ")
    .put("q", "ƌ")
    .put("r", "ȑ")
    .put("s", "ș")
    .put("t", "ț")
    .put("u", "ǚ")
    .put("v", "˄")
    .put("w", "ŵ")
    .put("x", "ˣ")
    .put("y", "ʎ")
    .put("z", "ź")
    .put("ä", "å")
    .put("ö", "õ")
    .put("ü", "û")
    .put("A", "Â")
    .put("B", "ß")
    .put("C", "Ç")
    .put("D", "Ð")
    .put("E", "Ë")
    .put("F", "Ƹ")
    .put("G", "Ġ")
    .put("H", "Ħ")
    .put("I", "Ĭ")
    .put("J", "Ĵ")
    .put("K", "Ķ")
    .put("L", "Ŀ")
    .put("M", "Щ")
    .put("N", "Ň")
    .put("O", "Õ")
    .put("P", "Þ")
    .put("Q", "Ƣ")
    .put("R", "Ȑ")
    .put("S", "Ș")
    .put("T", "Ʈ")
    .put("U", "Ǚ")
    .put("V", "Ʌ")
    .put("W", "ʩ")
    .put("X", "Ӿ")
    .put("Y", "Ӌ")
    .put("Z", "Ϟ")
    .put("Ä", "Ā")
    .put("Ö", "Ō")
    .put("Ü", "Ŭ")
    .put("0", "♡")
    .put("1", "Ⅰ")
    .put("2", "Ⅱ")
    .put("3", "Ⅲ")
    .put("4", "Ⅳ")
    .put("5", "Ⅴ")
    .put("6", "Ⅵ")
    .put("7", "Ⅶ")
    .put("8", "Ⅷ")
    .put("9", "Ⅸ")
    .put("_", "☂")
    .build();

  private TranslationUtil() {
    // Utility class
  }

  private static String translate(String sourceContent) {
    return sourceContent.chars()
      .mapToObj(i -> Character.toString((char) i))
      .map(TranslationUtil::translateCharacter)
      .collect(joining());
  }

  private static String translateCharacter(String s) {
    if (TRANSLATE.containsKey(s)) {
      return TRANSLATE.get(s);
    }
    return s;
  }

  /**
   * String will be parsed and for each node "trans-unit" the "target" node's
   * text content will be replaced by pseudo-translated content.
   *
   * @param untranslatedXliff the XLIFF to be translated
   * @param interceptor       an optional interceptor to modify the translation result
   * @return the pseudo-translated XLIFF
   */
  static String translateXliff(String untranslatedXliff, TranslationInterceptor interceptor) {
    Document doc = parseXliff(untranslatedXliff);
    performPseudoTranslation(doc, interceptor);
    try {
      return convertToString(doc);
    } catch (Exception e) {
      LOG.warn("Ignoring exception.", e);
    }
    return untranslatedXliff;
  }

  private static String convertToString(Document doc) throws TransformerException {
    Source source = new DOMSource(doc);
    Writer writer = new StringWriter();
    Result result = new StreamResult(writer);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Transformer transformer = transformerFactory.newTransformer();
    transformer.transform(source, result);
    return writer.toString();
  }

  // jspecify-reference-checker: Fails to deal with instanceof pattern variable. Suppressed.
  @SuppressWarnings("nullness")
  private static void performPseudoTranslation(Document doc, TranslationInterceptor interceptor) {
    NodeList elementsByTagName = doc.getElementsByTagName("trans-unit");
    for (int i = 0; i < elementsByTagName.getLength(); i++) {
      Node transUnitNode = elementsByTagName.item(i);
      if (transUnitNode instanceof Element transUnitElement) {
        Node targetNode = transUnitElement.getElementsByTagName("target").item(0);
        if (targetNode != null) {
          String targetContent = targetNode.getTextContent();
          String pseudoTranslated = interceptor.postTranslate(translate(targetContent));
          targetNode.setTextContent(pseudoTranslated);
          LOG.debug("Pseudo-translated {} to {}.", targetContent, pseudoTranslated);
        } else {
          LOG.warn("Expected pre-populated target element missing for trans-unit element {}. Pseudo-translation omitted.", i);
        }
      } else {
        throw new IllegalStateException("Unexpected trans-unit node type (expected: Element): " + transUnitNode);
      }
    }

    interceptor.postTranslate(doc);
  }

  private static Document parseXliff(String untranslatedXliff) {
    try {
      DocumentBuilderFactory factory = newDocumentBuilderFactory();
      DocumentBuilder builder;
      builder = factory.newDocumentBuilder();
      InputSource src = new InputSource(new StringReader(untranslatedXliff));
      return builder.parse(src);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException("Failed to parse XLIFF: " + untranslatedXliff, e);
    }
  }

  @SuppressWarnings("HttpUrlsUsage")
  private static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory;
  }
}
