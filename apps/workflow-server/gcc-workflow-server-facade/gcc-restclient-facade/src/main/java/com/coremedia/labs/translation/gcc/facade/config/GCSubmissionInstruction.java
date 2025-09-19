package com.coremedia.labs.translation.gcc.facade.config;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Behavioral configuration for submission instructions.
 * <p>
 * <strong>Example Configuration</strong>: (represents defaults)
 * <pre>{@code
 * globalLink:
 *   submissionInstruction:
 *     characterType: bmp
 *     characterReplacementStrategy: unicode-code-point
 * }</pre>
 *
 * @see CharacterType
 * @see CharacterReplacementStrategy
 * @since 2406.1
 */
@NullMarked
public final class GCSubmissionInstruction {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final String CONFIG_KEY = GCConfigProperty.KEY_SUBMISSION_INSTRUCTION;
  /**
   * The configuration key for the character type configuration.
   * <p>
   * <strong>Type</strong>: {@code String}
   *
   * @see CharacterType
   */
  public static final String CHARACTER_TYPE_KEY = "characterType";
  /**
   * The configuration key for the character replacement strategy configuration.
   * <p>
   * <strong>Type</strong>: {@code String}
   *
   * @see CharacterReplacementStrategy
   */
  public static final String CHARACTER_REPLACEMENT_STRATEGY_KEY = "characterReplacementStrategy";
  /**
   * The configuration key for the text type configuration.
   * <p>
   * <strong>Type</strong>: {@code String}
   *
   * @see TextTransform
   */
  public static final String TEXT_TRANSFORM_KEY = "textTransform";
  private static final CharacterType DEFAULT_SUPPORTED_CHARACTER_TYPE = CharacterType.BMP;
  private static final CharacterReplacementStrategy DEFAULT_CHARACTER_REPLACEMENT_STRATEGY = CharacterReplacementStrategy.UNICODE_CODE_POINT;
  private static final TextTransform DEFAULT_TEXT_TYPE = TextTransform.TEXT_TO_HTML;

  /**
   * The default behavior for submission instructions.
   * <p>
   * Applied defaults:
   * <ul>
   *   <li>{@code characterType}: {@link CharacterType#BMP}</li>
   *   <li>{@code characterReplacementStrategy}: {@link CharacterReplacementStrategy#UNICODE_CODE_POINT}</li>
   *   <li>{@code textTransform}: {@link TextTransform#TEXT_TO_HTML}</li>
   * </ul>
   */
  public static final GCSubmissionInstruction DEFAULT = new GCSubmissionInstruction(
    DEFAULT_SUPPORTED_CHARACTER_TYPE,
    DEFAULT_CHARACTER_REPLACEMENT_STRATEGY,
    DEFAULT_TEXT_TYPE
  );
  private final CharacterType characterType;
  private final CharacterReplacementStrategy characterReplacementStrategy;
  private final TextTransform textTransform;

  /**
   * Constructor.
   *
   * @param characterType                type of supported characters
   * @param characterReplacementStrategy strategy for replacing invalid characters
   * @param textTransform                     the default text type
   */
  private GCSubmissionInstruction(CharacterType characterType,
                                  CharacterReplacementStrategy characterReplacementStrategy,
                                  TextTransform textTransform) {
    this.characterType = characterType;
    this.characterReplacementStrategy = characterReplacementStrategy;
    this.textTransform = textTransform;
  }

  /**
   * Transforms the given plain-text according to the configuration.
   *
   * @param value the value to transform
   * @return the transformed value
   */
  public String transformText(String value) {
    String transformedCharacters = characterType.replaceAllInvalid(value, characterReplacementStrategy.replacer());
    String transformedText = textTransform.transform(transformedCharacters);
    if (LOG.isDebugEnabled() && !value.equals(transformedText)) {
      LOG.debug("Transformed submission instruction from '{}' to '{}'.", value, transformedText);
    }
    return transformedText;
  }

  /**
   * Returns the configuration for submission names from the given
   * {@code globalLink} configuration (sub-struct).
   *
   * @param config the {@code globalLink} configuration
   * @return the configuration for submission names
   */
  // jspecify-reference-checker: Fails to deal with pattern binding. Suppressed.
  @SuppressWarnings("nullness")
  public static GCSubmissionInstruction fromGlobalLinkConfig(Map<String, ?> config) {
    Object configObject = config.get(CONFIG_KEY);
    if (configObject instanceof GCSubmissionInstruction submissionInstruction) {
      return submissionInstruction;
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
  private static GCSubmissionInstruction fromSubmissionNameConfig(Map<?, ?> configMap) {
    if (configMap.isEmpty()) {
      return DEFAULT;
    }
    return new GCSubmissionInstruction(
      CharacterType.fromConfig(configMap.get(CHARACTER_TYPE_KEY)).orElse(DEFAULT_SUPPORTED_CHARACTER_TYPE),
      CharacterReplacementStrategy.fromConfig(configMap.get(CHARACTER_REPLACEMENT_STRATEGY_KEY)).orElse(DEFAULT_CHARACTER_REPLACEMENT_STRATEGY),
      TextTransform.fromConfig(configMap.get(TEXT_TRANSFORM_KEY)).orElse(DEFAULT_TEXT_TYPE)
    );
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    GCSubmissionInstruction that = (GCSubmissionInstruction) object;
    return characterType == that.characterType && characterReplacementStrategy == that.characterReplacementStrategy && textTransform == that.textTransform;
  }

  @Override
  public int hashCode() {
    return Objects.hash(characterType, characterReplacementStrategy, textTransform);
  }

  @Override
  public String toString() {
    return "%s[characterReplacementStrategy=%s, characterType=%s, textTransform=%s]".formatted(lookup().lookupClass().getSimpleName(), characterReplacementStrategy, characterType, textTransform);
  }
}
