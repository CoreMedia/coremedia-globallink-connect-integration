package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConfigException;
import com.coremedia.labs.translation.gcc.util.Settings;
import org.gs4tr.gcc.restclient.GCConfig;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Tests {@link GCConfigUtil}.
 */
@NullMarked
class GCConfigUtilTest {
  private static final String MOCK_URL = "https://example.com/api";
  private static final String MOCK_API_KEY = "test-api-key";
  private static final String MOCK_CONNECTOR_KEY = "test-connector-key";

  @Nested
  @DisplayName("Tests for fromGlobalLinkConfig")
  class FromGlobalLinkConfig {
    @Test
    @DisplayName("Should create GCConfig with all required properties")
    void shouldCreateGCConfigWithRequiredProperties() {
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY
      );

      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertSoftly(softly -> {
        softly.assertThat(gcConfig.getApiUrl()).isEqualTo(MOCK_URL);
        softly.assertThat(gcConfig.getApiKey()).isEqualTo(MOCK_API_KEY);
        softly.assertThat(gcConfig.getConnectorKey()).isEqualTo(MOCK_CONNECTOR_KEY);
        softly.assertThat(gcConfig.getUserAgent()).isEqualTo(GCConfigUtil.USER_AGENT);
      });
    }

    @ParameterizedTest(name = "[{index}] Missing required property: ''{0}''")
    @DisplayName("Should throw GCFacadeConfigException when required property is missing")
    @ValueSource(strings = {
      GCConfigProperty.KEY_URL,
      GCConfigProperty.KEY_API_KEY,
      GCConfigProperty.KEY_KEY
    })
    void shouldThrowExceptionWhenRequiredPropertyIsMissing(String missingKey) {
      Map<String, Object> config = new HashMap<>(Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY
      ));
      config.remove(missingKey);

      assertThatThrownBy(() -> GCConfigUtil.fromGlobalLinkConfig(new Settings(config)))
        .isInstanceOf(GCFacadeConfigException.class)
        .hasMessageContaining(missingKey);
    }

    @Test
    @DisplayName("Should configure max retries on service unavailable when provided")
    void shouldConfigureMaxRetriesOnServiceUnavailable() {
      int expectedRetries = 5;
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY,
        GCConfigProperty.KEY_MAX_RETRIES_ON_SERVICE_UNAVAILABLE, expectedRetries
      );

      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertThat(gcConfig.getMaxRetriesOnServiceUnavailable()).isEqualTo(expectedRetries);
    }

    @Test
    @DisplayName("Should configure max retries on request errors when provided")
    void shouldConfigureMaxRetriesOnRequestErrors() {
      int expectedRetries = 3;
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY,
        GCConfigProperty.KEY_MAX_RETRIES_ON_REQUEST_ERRORS, expectedRetries
      );

      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertThat(gcConfig.getMaxRetriesOnRequestErrors()).isEqualTo(expectedRetries);
    }

    @Test
    @DisplayName("Should configure both retry settings when provided")
    void shouldConfigureBothRetrySettings() {
      int expectedServiceUnavailableRetries = 5;
      int expectedRequestErrorRetries = 3;
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY,
        GCConfigProperty.KEY_MAX_RETRIES_ON_SERVICE_UNAVAILABLE, expectedServiceUnavailableRetries,
        GCConfigProperty.KEY_MAX_RETRIES_ON_REQUEST_ERRORS, expectedRequestErrorRetries
      );

      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertSoftly(softly -> {
        softly.assertThat(gcConfig.getMaxRetriesOnServiceUnavailable()).isEqualTo(expectedServiceUnavailableRetries);
        softly.assertThat(gcConfig.getMaxRetriesOnRequestErrors()).isEqualTo(expectedRequestErrorRetries);
      });
    }

    @Test
    @DisplayName("Should handle retry settings provided as strings")
    void shouldHandleRetrySettingsProvidedAsStrings() {
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY,
        GCConfigProperty.KEY_MAX_RETRIES_ON_SERVICE_UNAVAILABLE, "5",
        GCConfigProperty.KEY_MAX_RETRIES_ON_REQUEST_ERRORS, "3"
      );

      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertSoftly(softly -> {
        softly.assertThat(gcConfig.getMaxRetriesOnServiceUnavailable()).isEqualTo(5);
        softly.assertThat(gcConfig.getMaxRetriesOnRequestErrors()).isEqualTo(3);
      });
    }

    @Test
    @DisplayName("Should ignore invalid retry settings")
    void shouldIgnoreInvalidRetrySettings() {
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY,
        GCConfigProperty.KEY_MAX_RETRIES_ON_SERVICE_UNAVAILABLE, "invalid",
        GCConfigProperty.KEY_MAX_RETRIES_ON_REQUEST_ERRORS, "not-a-number"
      );

      // Should not throw exception, just ignore invalid values
      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      // GCConfig should have default values for retries
      assertSoftly(softly -> {
        softly.assertThat(gcConfig.getMaxRetriesOnServiceUnavailable()).isEqualTo(0);
        softly.assertThat(gcConfig.getMaxRetriesOnRequestErrors()).isEqualTo(0);
      });
    }

    @Test
    @DisplayName("Should set up logging redirection to SLF4J")
    void shouldSetUpLoggingRedirection() {
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY
      );

      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertThat(gcConfig.getLogger())
        .extracting(Logger::getHandlers, array(Handler[].class))
        .anySatisfy(h -> assertThat(h).isInstanceOf(SLF4JHandler.class));
    }
  }

  @Nested
  @DisplayName("Tests for tryParse")
  class TryParse {
    @Test
    @DisplayName("Should return null for null input")
    void shouldReturnNullForNullInput() {
      Integer result = GCConfigUtil.tryParse(null);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return integer value when input is already an Integer")
    void shouldReturnIntegerValueWhenInputIsInteger() {
      Integer input = 42;

      Integer result = GCConfigUtil.tryParse(input);

      assertThat(result).isEqualTo(42);
    }

    @ParameterizedTest(name = "[{index}] Parse string: ''{0}'' -> {1}")
    @DisplayName("Should parse valid integer strings")
    @MethodSource("validIntegerStrings")
    void shouldParseValidIntegerStrings(String input, int expected) {
      Integer result = GCConfigUtil.tryParse(input);

      assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] Invalid string: ''{0}''")
    @DisplayName("Should return null for invalid integer strings")
    @ValueSource(strings = {
      "not-a-number",
      "12.34",
      "1e10",
      "infinity",
      "",
      " ",
      "0x10"
    })
    void shouldReturnNullForInvalidIntegerStrings(String input) {
      Integer result = GCConfigUtil.tryParse(input);

      assertThat(result).isNull();
    }

    @ParameterizedTest(name = "[{index}] Unexpected type: {0}")
    @DisplayName("Should return null for unexpected types")
    @MethodSource("unexpectedTypes")
    void shouldReturnNullForUnexpectedTypes(Object input) {
      Integer result = GCConfigUtil.tryParse(input);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle zero")
    void shouldHandleZero() {
      Integer result = GCConfigUtil.tryParse("0");

      assertThat(result).isZero();
    }

    @Test
    @DisplayName("Should handle negative integers")
    void shouldHandleNegativeIntegers() {
      Integer result = GCConfigUtil.tryParse("-42");

      assertThat(result).isEqualTo(-42);
    }

    @Test
    @DisplayName("Should handle Integer.MAX_VALUE")
    void shouldHandleMaxValue() {
      Integer result = GCConfigUtil.tryParse(String.valueOf(Integer.MAX_VALUE));

      assertThat(result).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("Should handle Integer.MIN_VALUE")
    void shouldHandleMinValue() {
      Integer result = GCConfigUtil.tryParse(String.valueOf(Integer.MIN_VALUE));

      assertThat(result).isEqualTo(Integer.MIN_VALUE);
    }

    @Test
    @DisplayName("Should return null for numbers outside Integer range")
    void shouldReturnNullForNumbersOutsideRange() {
      String tooLarge = String.valueOf((long) Integer.MAX_VALUE + 1L);

      Integer result = GCConfigUtil.tryParse(tooLarge);

      assertThat(result).isNull();
    }

    static Stream<Arguments> validIntegerStrings() {
      return Stream.of(
        Arguments.of("0", 0),
        Arguments.of("1", 1),
        Arguments.of("42", 42),
        Arguments.of("100", 100),
        Arguments.of("-1", -1),
        Arguments.of("-42", -42),
        Arguments.of(String.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE),
        Arguments.of(String.valueOf(Integer.MIN_VALUE), Integer.MIN_VALUE)
      );
    }

    static Stream<Arguments> unexpectedTypes() {
      return Stream.of(
        Arguments.of(42.5),
        Arguments.of(42L),
        Arguments.of(true),
        Arguments.of(false),
        Arguments.of(new Object()),
        Arguments.of((Object) new int[]{1, 2, 3}),
        Arguments.of(Map.of("key", "value"))
      );
    }
  }

  @Nested
  @DisplayName("Tests for USER_AGENT constant")
  class UserAgent {
    @Test
    @DisplayName("USER_AGENT should be set to package name")
    void userAgentShouldBeSetToPackageName() {
      assertThat(GCConfigUtil.USER_AGENT)
        .isEqualTo("com.coremedia.labs.translation.gcc.facade.def");
    }
  }

  @Nested
  @DisplayName("Integration tests")
  class IntegrationTests {
    @Test
    @DisplayName("Should create fully configured GCConfig with all optional settings")
    void shouldCreateFullyConfiguredGCConfig() {
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY,
        GCConfigProperty.KEY_MAX_RETRIES_ON_SERVICE_UNAVAILABLE, 10,
        GCConfigProperty.KEY_MAX_RETRIES_ON_REQUEST_ERRORS, 7
      );

      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertSoftly(softly -> {
          softly.assertThat(gcConfig.getApiUrl()).isEqualTo(MOCK_URL);
          softly.assertThat(gcConfig.getApiKey()).isEqualTo(MOCK_API_KEY);
          softly.assertThat(gcConfig.getConnectorKey()).isEqualTo(MOCK_CONNECTOR_KEY);
          softly.assertThat(gcConfig.getUserAgent()).isEqualTo(GCConfigUtil.USER_AGENT);
          softly.assertThat(gcConfig.getMaxRetriesOnServiceUnavailable()).isEqualTo(10);
          softly.assertThat(gcConfig.getMaxRetriesOnRequestErrors()).isEqualTo(7);
          softly.assertThat(gcConfig.getLogger()).isNotNull();
      });
    }

    @Test
    @DisplayName("Should handle Settings with nested structure")
    void shouldHandleSettingsWithNestedStructure() {
      Map<String, Object> config = Map.of(
        GCConfigProperty.KEY_URL, MOCK_URL,
        GCConfigProperty.KEY_API_KEY, MOCK_API_KEY,
        GCConfigProperty.KEY_KEY, MOCK_CONNECTOR_KEY,
        "nested", Map.of("key", "value")
      );

      // Should not throw exception even with extra nested properties
      GCConfig gcConfig = GCConfigUtil.fromGlobalLinkConfig(new Settings(config));

      assertThat(gcConfig).isNotNull();
    }
  }
}

