package com.coremedia.labs.translation.gcc.facade.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A text transformation to apply from CoreMedia CMS to, for example,
 * submission data in GCC backend.
 * <p>
 * Meant to be used, for example, for plain-text workflow notes (CMS) send over
 * as submission instructions (GCC), which expects the text to be in HTML.
 *
 * @since 2406.1
 */
public enum TextTransform {
  /**
   * No transformation. Take as is.
   */
  NONE,
  /**
   * Signals HTML content.
   */
  TEXT_TO_HTML {
    private static final String SPACE_INDENT = "&nbsp;";
    private static final String TAB_INDENT = "&nbsp;&nbsp;&nbsp;&nbsp;";
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R");
    private static final Pattern LEADING_SPACES = Pattern.compile("(?m)^ +");

    /**
     * Transforms the given plain-text to the expected HTML.
     * <p>
     * Despite the general entity encoding, tabs are replaced by a number of
     * non-breaking spaces and newlines are replaced by HTML line breaks.
     *
     * @param text the text to transform
     * @return the transformed text
     */
    @Override
    @NonNull
    public String transform(@NonNull String text) {
      String result = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("\t", TAB_INDENT);
      // For all leading spaces in each line, replace with non-breaking spaces.
      // This is meant to keep the indentation.
      result = LEADING_SPACES.matcher(result)
        .replaceAll(match -> SPACE_INDENT.repeat(match.group().length()));
      return NEWLINE_PATTERN.matcher(result).replaceAll("<br>");
    }
  },
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());
  @NonNull
  private final String id;

  TextTransform() {
    id = stripUnderscoresAndDashes(name());
  }

  /**
   * Transforms the given text to the target type.
   *
   * @param text the text to transform
   * @return the transformed text
   */
  @NonNull
  public String transform(@NonNull String text) {
    return text;
  }

  /**
   * Returns the type for the given configuration object.
   *
   * @param type type as object
   * @return the parsed type, or empty if the type is unknown, not set, or of
   * an unsupported type
   */
  public static Optional<TextTransform> fromConfig(@Nullable Object type) {
    if (type == null) {
      LOG.trace("No text-type given. Returning empty.");
      return Optional.empty();
    }
    if (type instanceof TextTransform textTransform) {
      return Optional.of(textTransform);
    }
    if (type instanceof String stringType) {
      return fromString(stringType);
    }
    LOG.debug("Unsupported type of text-type {} '{}'. Returning empty.", type.getClass(), type);
    return Optional.empty();
  }

  /**
   * Returns the type for the given string. Case-insensitive.
   *
   * @param type type as string
   * @return the parsed type
   * set
   */
  @NonNull
  public static Optional<TextTransform> fromString(@Nullable String type) {
    if (type == null || type.isBlank()) {
      LOG.debug("Empty transformation type. Returning empty.");
      return Optional.empty();
    }
    String trimmedType = stripUnderscoresAndDashes(type.trim());
    for (TextTransform value : values()) {
      if (value.id.equalsIgnoreCase(trimmedType)) {
        return Optional.of(value);
      }
    }
    LOG.debug("Unknown transformation type '{}'. Returning empty.", type);
    return Optional.empty();
  }

  /**
   * Used to eventually support camel-case, snake-case, and kebab-case.
   *
   * @param str string to strip underscores and dashes from
   * @return string without underscores and dashes
   */
  @NonNull
  private static String stripUnderscoresAndDashes(@NonNull String str) {
    return str.replace("_", "").replace("-", "");
  }
}
