package com.coremedia.labs.translation.gcc.facade.config;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Behavioral configuration for submission names.
 * <p>
 * <strong>Example Configuration</strong>: (represents defaults)
 * <pre>{@code
 * globalLink:
 *   submissionName:
 *     characterType: bmp
 *     characterReplacementStrategy: underscore
 * }</pre>
 *
 * @see CharacterType
 * @see CharacterReplacementStrategy
 * @since 2406.1
 */
public final class GCSubmissionName {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final String CONFIG_KEY = GCConfigProperty.KEY_SUBMISSION_NAME;
  public static final String CHARACTER_TYPE_KEY = "characterType";
  public static final String CHARACTER_REPLACEMENT_STRATEGY_KEY = "characterReplacementStrategy";
  private static final CharacterType DEFAULT_SUPPORTED_CHARACTER_TYPE = CharacterType.BMP;
  private static final CharacterReplacementStrategy DEFAULT_CHARACTER_REPLACEMENT_STRATEGY = CharacterReplacementStrategy.UNDERSCORE;
  /**
   * Maximum length of submission name.
   */
  // DevNote: Not yet configurable.
  public static final int DEFAULT_MAX_LENGTH = 255;

  /**
   * The default behavior for submission names.
   * <p>
   * Applied defaults:
   * <ul>
   *   <li>{@code characterType}: {@link CharacterType#BMP}</li>
   *   <li>{@code characterReplacementStrategy}: {@link CharacterReplacementStrategy#UNDERSCORE}</li>
   * </ul>
   */
  @NonNull
  public static final GCSubmissionName DEFAULT = new GCSubmissionName(DEFAULT_SUPPORTED_CHARACTER_TYPE, DEFAULT_CHARACTER_REPLACEMENT_STRATEGY);
  @NonNull
  private final CharacterType characterType;
  @NonNull
  private final CharacterReplacementStrategy characterReplacementStrategy;

  /**
   * Constructor.
   *
   * @param characterType                type of supported characters
   * @param characterReplacementStrategy strategy for replacing invalid characters
   */
  private GCSubmissionName(@NonNull CharacterType characterType,
                           @NonNull CharacterReplacementStrategy characterReplacementStrategy) {
    this.characterType = characterType;
    this.characterReplacementStrategy = characterReplacementStrategy;
  }

  /**
   * Transforms the given value according to the configuration.
   *
   * @param value the value to transform
   * @return the transformed value
   */
  @NonNull
  public String transform(@NonNull String value) {
    String trimmed = value.trim();
    String transformed = characterType.replaceAllInvalid(trimmed, characterReplacementStrategy.replacer());
    String truncated = truncate(transformed);
    if (LOG.isDebugEnabled() && !value.equals(truncated)) {
      LOG.debug("Transformed submission name from '{}' to '{}'.", value, truncated);
    }
    return truncated;
  }

  /**
   * Truncates the given value to the maximum length.
   *
   * @param value the value to truncate
   * @return the truncated value
   */
  @NonNull
  private static String truncate(@NonNull String value) {
    return value.substring(0, Math.min(value.length(), DEFAULT_MAX_LENGTH));
  }

  /**
   * Returns the configuration for submission names from the given
   * {@code globalLink} configuration (sub-struct).
   *
   * @param config the {@code globalLink} configuration
   * @return the configuration for submission names
   */
  @NonNull
  public static GCSubmissionName fromGlobalLinkConfig(@NonNull Map<String, ?> config) {
    Object configObject = config.get(CONFIG_KEY);
    if (configObject instanceof GCSubmissionName submissionName) {
      return submissionName;
    }
    if (configObject instanceof Map<?, ?> configMap) {
      return fromSubmissionNameConfig(configMap);
    }
    return DEFAULT;
  }

  /**
   * Returns the configuration for submission names from the given
   * configuration map.
   *
   * @param configMap the configuration map
   * @return the configuration for submission names
   */
  private static GCSubmissionName fromSubmissionNameConfig(Map<?, ?> configMap) {
    if (configMap.isEmpty()) {
      return DEFAULT;
    }
    return new GCSubmissionName(
      CharacterType.fromConfig(configMap.get(CHARACTER_TYPE_KEY)).orElse(DEFAULT_SUPPORTED_CHARACTER_TYPE),
      CharacterReplacementStrategy.fromConfig(configMap.get(CHARACTER_REPLACEMENT_STRATEGY_KEY)).orElse(DEFAULT_CHARACTER_REPLACEMENT_STRATEGY)
    );
  }

  @Override
  public boolean equals(Object object) {
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    GCSubmissionName that = (GCSubmissionName) object;
    return characterType == that.characterType && characterReplacementStrategy == that.characterReplacementStrategy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(characterType, characterReplacementStrategy);
  }

  @Override
  public String toString() {
    return "%s[characterReplacementStrategy=%s, characterType=%s]".formatted(lookup().lookupClass().getSimpleName(), characterReplacementStrategy, characterType);
  }
}
