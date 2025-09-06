package com.coremedia.labs.translation.gcc.facade.mock.settings;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Settings for the Mock Facade.
 */
@NullMarked
public record MockSettings(
  long stateChangeDelaySeconds,
  int stateChangeDelayOffsetPercentage,
  @Nullable MockError error,
  MockSubmissionStates submissionStates
) {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final long DEFAULT_STATE_CHANGE_DELAY_SECONDS = 120L;
  private static final int DEFAULT_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE = 50;
  /**
   * Empty settings (with defaults applied).
   */
  public static final MockSettings EMPTY = new MockSettings(
    DEFAULT_STATE_CHANGE_DELAY_SECONDS,
    DEFAULT_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE,
    null,
    MockSubmissionStates.EMPTY
  );

  /**
   * Legacy top-level configuration key below `globalLink` setting.
   *
   * @deprecated Use mock.{@value #CONFIG_STATE_CHANGE_DELAY_SECONDS} instead.
   */
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated(since = "2406.1")
  public static final String LEGACY_CONFIG_DELAY_SECONDS = "mockDelaySeconds";
  /**
   * Legacy top-level configuration key below `globalLink` setting.
   *
   * @deprecated Use mock.{@value #CONFIG_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE} instead.
   */
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated(since = "2406.1")
  public static final String LEGACY_CONFIG_DELAY_OFFSET_PERCENTAGE = "mockDelayOffsetPercentage";
  /**
   * Legacy top-level configuration key below `globalLink` setting.
   *
   * @deprecated Use mock.{@value #CONFIG_ERROR} instead.
   */
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated(since = "2406.1")
  public static final String LEGACY_CONFIG_MOCK_ERROR = "mockError";

  /**
   * Struct key for the mock configuration.
   */
  public static final String CONFIG_MOCK = "mock";

  /**
   * The state change delay for newly created tasks (minimum) in seconds.
   * Note, that it will get adapted by some random offset as configured by
   * {@link #CONFIG_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE}.
   */
  public static final String CONFIG_STATE_CHANGE_DELAY_SECONDS = "stateChangeDelaySeconds";
  /**
   * Random offset for state change delay for newly created tasks in percent.
   * The offset is applied to the delay configured by
   * {@link #CONFIG_STATE_CHANGE_DELAY_SECONDS}.
   */
  public static final String CONFIG_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE = "stateChangeDelayOffsetPercentage";
  /**
   * Provokes a specific error in the mock facade, like, for example,
   * communication errors during XLIFF download.
   * <p>
   * Applicable values are the names of the enum values of
   * {@link MockError}
   * (case-insensitive).
   */
  public static final String CONFIG_ERROR = "error";
  /**
   * Augments artificial submission state changes.
   */
  public static final String CONFIG_SUBMISSION_STATES = "submissionStates";

  public MockSettings {
    Objects.requireNonNull(submissionStates, "submissionStates");
    if (stateChangeDelayOffsetPercentage < 0 || stateChangeDelayOffsetPercentage > 100) {
      throw new IllegalArgumentException("Offset Percentage must be between 0 and 100.");
    }
  }

  public static MockSettings fromGlobalLinkConfig(Map<String, ?> config) {
    Object mockConfigObject = config.get(CONFIG_MOCK);
    if (mockConfigObject instanceof Map<?, ?> mockConfigMap) {
      return fromMockConfig(mockConfigMap);
    }
    return EMPTY;
  }

  public static MockSettings fromMockConfig(Map<?, ?> config) {
    if (config.isEmpty()) {
      return EMPTY;
    }
    long stateChangeDelaySeconds = DEFAULT_STATE_CHANGE_DELAY_SECONDS;
    int stateChangeDelayOffsetPercentage = DEFAULT_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE;
    MockError error = null;
    MockSubmissionStates submissionStates = MockSubmissionStates.EMPTY;

    Object stateChangeDelaySecondsObject = config.get(CONFIG_STATE_CHANGE_DELAY_SECONDS);
    if (stateChangeDelaySecondsObject == null) {
      stateChangeDelaySecondsObject = config.get(LEGACY_CONFIG_DELAY_SECONDS);
    }
    if (stateChangeDelaySecondsObject instanceof Number number) {
      stateChangeDelaySeconds = number.longValue();
    }

    Object stateChangeDelayOffsetPercentageObject = config.get(CONFIG_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE);
    if (stateChangeDelayOffsetPercentageObject == null) {
      stateChangeDelayOffsetPercentageObject = config.get(LEGACY_CONFIG_DELAY_OFFSET_PERCENTAGE);
    }
    if (stateChangeDelayOffsetPercentageObject instanceof Number number) {
      stateChangeDelayOffsetPercentage = number.intValue();
    }

    Object errorObject = config.get(CONFIG_ERROR);
    if (errorObject == null) {
      errorObject = config.get(LEGACY_CONFIG_MOCK_ERROR);
    }
    if (errorObject instanceof String errorString) {
      error = MockError.tryParse(errorString).orElse(null);
    }

    Object submissionStatesObject = config.get(CONFIG_SUBMISSION_STATES);
    if (submissionStatesObject instanceof Map<?, ?> submissionStatesMap) {
      submissionStates = MockSubmissionStates.fromConfig(submissionStatesMap);
    }

    MockSettings mockSettings = new MockSettings(
      stateChangeDelaySeconds,
      stateChangeDelayOffsetPercentage,
      error,
      submissionStates
    );
    LOG.debug("Parsed mock settings: {}", mockSettings);
    return mockSettings;
  }
}
