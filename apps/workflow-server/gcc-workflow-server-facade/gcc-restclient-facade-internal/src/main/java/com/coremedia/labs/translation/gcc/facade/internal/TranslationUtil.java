package com.coremedia.labs.translation.gcc.facade.internal;

import com.coremedia.blueprint.translation.TranslationService;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Provides simple <em>mock</em> translation by replacing characters.
 */
@DefaultAnnotation(NonNull.class)
final class TranslationUtil {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * String will be parsed and for each node "trans-unit" the "target" node's
   * text content will be replaced by pseudo-translated content.
   *
   * @param untranslatedXliff the XLIFF to be translated
   */
  static String translateXliff(String untranslatedXliff, TranslationService translationService) {
    Document doc = parseXliff(untranslatedXliff);
    performPseudoTranslation(doc, translationService);
    try {
      return convertToString(doc);
    } catch (Exception e) {
      LOG.warn("Ignoring exception.", e);
    }
    return untranslatedXliff;
  }

  private static String convertToString(Document doc) throws TransformerException {
    DOMSource source = new DOMSource(doc);
    Writer writer = new StringWriter();
    Result result = new StreamResult(writer);

    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    Transformer transformer = transformerFactory.newTransformer();
    transformer.transform(source, result);
    return writer.toString();
  }

  private static void performPseudoTranslation(Document doc, TranslationService translationService) {
    NodeList files = doc.getElementsByTagName("file");
    Optional<String> sourceLanguage = Optional.empty();
    Optional<String> targetLanguage = Optional.empty();
    for (int i = 0; i < files.getLength(); i++) {
      Node file = files.item(i);
      if (file.getAttributes() != null) {
        sourceLanguage = localeFromAttributes("source-language", file);
        targetLanguage = localeFromAttributes("target-language", file);
      }
    }
    if (sourceLanguage.isPresent() && targetLanguage.isPresent()) {
      NodeList elementsByTagName = doc.getElementsByTagName("trans-unit");
      for (int i = 0; i < elementsByTagName.getLength(); i++) {
        Node transUnitNode = elementsByTagName.item(i);
        if (transUnitNode instanceof Element) {
          Element transUnitElement = (Element) transUnitNode;
          Node sourceNode = transUnitElement.getElementsByTagName("source").item(0);
          Node targetNode = transUnitElement.getElementsByTagName("target").item(0);
          if (sourceNode != null && targetNode != null) {
            String sourceNodeTextContent = sourceNode.getTextContent();
            String pseudoTranslated = translationService.translate(sourceNodeTextContent, sourceLanguage.get(), targetLanguage.get(), false).orElse(sourceNodeTextContent);
            targetNode.setTextContent(pseudoTranslated);
            LOG.debug("Pseudo-translated {} to {}.", sourceNodeTextContent, pseudoTranslated);
          } else {
            LOG.warn("Expected pre-populated target element missing for trans-unit element {}. Pseudo-translation omitted.", i);
          }
        } else {
          throw new IllegalStateException("Unexpected trans-unit node type (expected: Element): " + transUnitNode);
        }
      }
    }
  }

  private static Optional<String> localeFromAttributes(String key, Node file) {
    if (file.getAttributes().getNamedItem(key) != null) {
      return Optional.of(Locale.forLanguageTag(file.getAttributes().getNamedItem(key).getNodeValue()).getLanguage());
    }
    return Optional.empty();
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

  private static DocumentBuilderFactory newDocumentBuilderFactory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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
