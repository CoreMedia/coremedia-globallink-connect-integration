package com.coremedia.labs.translation.gcc.facade.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The expected type of text within the GCC backend.
 *
 * @since 2406.1
 */
public enum TextType {
  /**
   * Signals HTML content.
   */
  HTML {
    private static final String SPACE_INDENT = "&nbsp;";
    private static final String TAB_INDENT = "&nbsp;&nbsp;&nbsp;&nbsp;";
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\R");
    private static final Pattern LEADING_SPACES = Pattern.compile("(?m)^\\s+");

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
    public String transformText(@NonNull String text) {
      String result = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("\t", TAB_INDENT);
      // For all leading spaces in each line, replace with non-breaking spaces.
      // This is meant to keep the indentation.
      result = LEADING_SPACES.matcher(result).replaceAll(SPACE_INDENT);
      return NEWLINE_PATTERN.matcher(result).replaceAll("<br>");
    }
  },
  /**
   * Signals plain text content.
   */
  TEXT,
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * Transforms the given plain-text to the expected type.
   *
   * @param text the text to transform
   * @return the transformed text
   */
  @NonNull
  public String transformText(@NonNull String text) {
    return text;
  }

  /**
   * Returns the type for the given configuration object.
   *
   * @param type type as object
   * @return the parsed type, or empty if the type is unknown, not set, or of
   * an unsupported type
   */
  public static Optional<TextType> fromConfig(@Nullable Object type) {
    if (type == null) {
      LOG.trace("No text-type given. Returning empty.");
      return Optional.empty();
    }
    if (type instanceof TextType textType) {
      return Optional.of(textType);
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
  public static Optional<TextType> fromString(@Nullable String type) {
    if (type == null || type.isBlank()) {
      LOG.debug("Empty text-type. Returning empty.");
      return Optional.empty();
    }
    String trimmedType = type.trim();
    for (TextType value : values()) {
      if (value.name().equalsIgnoreCase(trimmedType)) {
        return Optional.of(value);
      }
    }
    LOG.debug("Unknown text-type '{}'. Returning empty.", type);
    return Optional.empty();
  }
}
