package com.coremedia.labs.translation.gcc.facade.config;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Specifies the supported character set for strings and characters to
 * be transmitted to the GCC backend.
 * <p>
 * Some properties of submissions are limited in their supported character
 * set. For example, submission instructions as well as submission names
 * are limited to the Unicode Basic Multilingual Plane (BMP).
 * <p>
 * Others, like submitter names, are not limited to the BMP and may also use
 * characters in Unicode planes such as the Supplementary Multilingual Plane.
 *
 * @since 2406.1
 */
@NullMarked
public enum CharacterType {
  /**
   * Only characters from the Basic Multilingual Plane (BMP) are supported.
   */
  BMP {
    private static final Pattern HIGHER_UNICODE_CHARACTERS = Pattern.compile("[^\\x00-\\uffff]");

    @Override
    public String replaceAllInvalid(String value,
                                    Function<MatchResult, String> replacer) {
      return HIGHER_UNICODE_CHARACTERS
        .matcher(value)
        .replaceAll(replacer);
    }
  },
  /**
   * Full Unicode support.
   */
  UNICODE {
    /**
     * No replacement needed, as all characters are supported.
     *
     * @param value    the value to check
     * @param replacer the function to replace invalid characters; ignored
     * @return the value as is
     */
    @Override
    public String replaceAllInvalid(String value,
                                    Function<MatchResult, String> replacer) {
      return value;
    }
  },
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * Replaces all characters in the given string that are not supported
   * by the character set with the result of the given replacer function.
   *
   * @param value    the value to check
   * @param replacer the function to replace invalid characters
   * @return the value with all invalid characters replaced
   */
  public abstract String replaceAllInvalid(String value, Function<MatchResult, String> replacer);

  /**
   * Returns the type for the given configuration object.
   *
   * @param type type as object
   * @return the parsed type, or empty if the type is unknown, not set, or of
   * an unsupported type
   */
  public static Optional<CharacterType> fromConfig(@Nullable Object type) {
    switch (type) {
      case null -> {
        LOG.trace("No character-type given. Returning empty.");
        return Optional.empty();
      }
      case CharacterType characterType -> {
        return Optional.of(characterType);
      }
      case String stringType -> {
        return fromString(stringType);
      }
      default -> {
        LOG.debug("Unsupported type of character-type {} '{}'. Returning empty.", type.getClass(), type);
        return Optional.empty();
      }
    }
  }

  /**
   * Returns the type for the given string. Case-insensitive.
   *
   * @param type type as string
   * @return the parsed type, or empty if the type is unknown/not set
   */
  public static Optional<CharacterType> fromString(@Nullable String type) {
    if (type == null || type.isBlank()) {
      LOG.trace("No supported character-type given. Returning empty.");
      return Optional.empty();
    }
    String trimmedType = type.trim();
    for (CharacterType value : values()) {
      if (value.name().equalsIgnoreCase(trimmedType)) {
        return Optional.of(value);
      }
    }
    LOG.debug("Unknown character-type '{}'. Returning empty.", type);
    return Optional.empty();
  }
}
