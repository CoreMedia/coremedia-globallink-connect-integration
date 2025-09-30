package com.coremedia.labs.translation.gcc.facade.mock.settings;

import com.coremedia.labs.translation.gcc.facade.mock.scenarios.NoOperationScenario;
import com.coremedia.labs.translation.gcc.facade.mock.scenarios.Scenario;
import com.coremedia.labs.translation.gcc.facade.mock.scenarios.Scenarios;
import com.coremedia.labs.translation.gcc.util.Settings;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Settings for the Mock Facade.
 */
@NullMarked
public record MockSettings(
  long stateChangeDelaySeconds,
  int stateChangeDelayOffsetPercentage,
  Scenario scenario
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
    NoOperationScenario.INSTANCE
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
   * A scenario that should be mocked.
   *
   * @since 2506.0.1-1
   */
  // Planned to replace some of existing mocking strategies, like to
  // simulate errors.
  public static final String SCENARIO = "scenario";

  public MockSettings {
    if (stateChangeDelayOffsetPercentage < 0 || stateChangeDelayOffsetPercentage > 100) {
      throw new IllegalArgumentException("Offset Percentage must be between 0 and 100.");
    }
  }

  // jspecify-reference-checker: Fails to deal with instanceof pattern variable. Suppressed.
  @SuppressWarnings("nullness")
  public static MockSettings fromGlobalLinkConfig(Settings config) {
    Optional<Object> mockConfig = config.at(CONFIG_MOCK);
    if (mockConfig.isEmpty()) {
      return EMPTY;
    }
    Object mockConfigObject = mockConfig.get();
    if (mockConfigObject instanceof Map<?, ?> mockConfigMap) {
      return fromMockConfig(mockConfigMap);
    }
    return EMPTY;
  }

  // jspecify-reference-checker: Fails to deal with instanceof pattern variable. Suppressed.
  @SuppressWarnings("nullness")
  public static MockSettings fromMockConfig(Map<?, ?> config) {
    if (config.isEmpty()) {
      return EMPTY;
    }
    long stateChangeDelaySeconds = DEFAULT_STATE_CHANGE_DELAY_SECONDS;
    int stateChangeDelayOffsetPercentage = DEFAULT_STATE_CHANGE_DELAY_OFFSET_PERCENTAGE;
    Scenario scenario = NoOperationScenario.INSTANCE;

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

    Object scenarioObject = config.get(SCENARIO);
    if (scenarioObject instanceof String scenarioString) {
      scenario = Scenarios.fromString(scenarioString).orElse(NoOperationScenario.INSTANCE);
      LOG.info("Active scenario: {} ({})", scenario.id(), scenario.getClass().getName());
    }

    MockSettings mockSettings = new MockSettings(
      stateChangeDelaySeconds,
      stateChangeDelayOffsetPercentage,
      scenario
    );
    LOG.debug("Parsed mock settings: {}", mockSettings);
    return mockSettings;
  }
}
