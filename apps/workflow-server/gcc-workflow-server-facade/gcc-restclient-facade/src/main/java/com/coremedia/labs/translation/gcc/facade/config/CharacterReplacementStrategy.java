package com.coremedia.labs.translation.gcc.facade.config;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Pre-fabricated replacement-strategy.
 *
 * @since 2406.1
 */
@NullMarked
public enum CharacterReplacementStrategy {
  /**
   * Do not apply any replacement.
   */
  NONE() {
    @Override
    public Function<MatchResult, String> replacer() {
      return MatchResult::group;
    }
  },
  /**
   * Replace with an empty string.
   */
  EMPTY() {
    @Override
    public Function<MatchResult, String> replacer() {
      return mr -> "";
    }
  },
  /**
   * Replace with an underscore.
   */
  UNDERSCORE() {
    @Override
    public Function<MatchResult, String> replacer() {
      return mr -> replaceIfNotEmpty(mr, "_");
    }
  },
  /**
   * Replace with a question mark.
   */
  QUESTION_MARK() {
    @Override
    public Function<MatchResult, String> replacer() {
      return mr -> replaceIfNotEmpty(mr, "?");
    }
  },
  /**
   * Replace with a Unicode code point.
   */
  UNICODE_CODE_POINT() {
    @Override
    public Function<MatchResult, String> replacer() {
      return mr -> {
        String group = mr.group();
        if (group.isEmpty()) {
          return "";
        }
        int codePoint = group.codePointAt(0);
        return String.format("U+%04X", codePoint);
      };
    }
  },
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());

  private final String id;

  CharacterReplacementStrategy() {
    id = stripUnderscoresAndDashes(name());
  }

  public abstract Function<MatchResult, String> replacer();

  /**
   * Returns the strategy for the given configuration object.
   *
   * @param type type as object
   * @return the parsed strategy, or empty if the strategy is unknown, not set,
   * or of an unsupported type
   */
  public static Optional<CharacterReplacementStrategy> fromConfig(@Nullable Object type) {
    if (type == null) {
      LOG.trace("No replacement-strategy given. Returning empty.");
      return Optional.empty();
    }
    if (type instanceof CharacterReplacementStrategy strategy) {
      return Optional.of(strategy);
    }
    if (type instanceof String stringType) {
      return fromString(stringType);
    }
    LOG.debug("Unsupported type of replacement-strategy {} '{}'. Returning empty.", type.getClass(), type);
    return Optional.empty();
  }

  public static Optional<CharacterReplacementStrategy> fromString(@Nullable String strategy) {
    if (strategy == null || strategy.isBlank()) {
      LOG.trace("No replacement-strategy given. Returning empty.");
      return Optional.empty();
    }
    String alignedStrategy = stripUnderscoresAndDashes(strategy.trim());
    for (CharacterReplacementStrategy value : values()) {
      if (value.id.equalsIgnoreCase(alignedStrategy)) {
        return Optional.of(value);
      }
    }
    LOG.debug("Unknown replacement-strategy '{}'. Returning empty.", strategy);
    return Optional.empty();
  }

  private static String replaceIfNotEmpty(MatchResult mr, String replacement) {
    return mr.group().isEmpty() ? "" : replacement;
  }

  /**
   * Used to eventually support camel-case, snake-case, and kebab-case.
   *
   * @param str string to strip underscores and dashes from
   * @return string without underscores and dashes
   */
  private static String stripUnderscoresAndDashes(String str) {
    return str.replace("_", "").replace("-", "");
  }
}
