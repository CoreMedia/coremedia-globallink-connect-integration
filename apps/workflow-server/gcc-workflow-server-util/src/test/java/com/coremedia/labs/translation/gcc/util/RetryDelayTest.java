package com.coremedia.labs.translation.gcc.util;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

@NullMarked
class RetryDelayTest {
  @Nested
  class ConstructorBehavior {
    @ParameterizedTest
    @EnumSource(ValidDuration.class)
    void shouldAcceptDurationsInRange(ValidDuration fixture) {
      assertThatCode(() -> new RetryDelay(fixture.duration()))
        .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(InvalidDuration.class)
    void shouldDenyDurationOutOfBounds(InvalidDuration fixture) {
      assertThatThrownBy(() -> new RetryDelay(fixture.duration()))
        .hasMessageContainingAll("value", "than or equal to")
        .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class OfBehavior {
    @ParameterizedTest
    @EnumSource(ValidDuration.class)
    void shouldAcceptDurationsInRange(ValidDuration fixture) {
      assertThatCode(() -> RetryDelay.of(fixture.duration()))
        .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(InvalidDuration.class)
    void shouldDenyDurationOutOfBounds(InvalidDuration fixture) {
      assertThatThrownBy(() -> RetryDelay.of(fixture.duration()))
        .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class SaturatedOfBehavior {
    @ParameterizedTest
    @EnumSource(ValidDuration.class)
    void shouldAcceptDurationsInRange(ValidDuration fixture) {
      assertThatCode(() -> RetryDelay.saturatedOf(fixture.duration()))
        .doesNotThrowAnyException();
    }

    @Nested
    @ParameterizedClass
    @EnumSource(InvalidDuration.class)
    class OutOfBoundsBehavior {
      private final Duration durationFixture;

      OutOfBoundsBehavior(InvalidDuration invalidDuration) {
        durationFixture = invalidDuration.duration();
      }

      @Test
      void shouldAcceptDurationOutOfBounds() {
        assertThatCode(() -> RetryDelay.saturatedOf(durationFixture))
          .doesNotThrowAnyException();
      }

      @Test
      void shouldNormalizeToWithinBounds() {
        assertThat(RetryDelay.saturatedOf(durationFixture))
          .isBetween(RetryDelay.MIN_VALUE, RetryDelay.MAX_VALUE);
      }
    }
  }

  @Nested
  @ParameterizedClass
  @EnumSource(ValidDuration.class)
  class ToSecondsBehavior {
    private final RetryDelay delayFixture;
    private final Duration durationFixture;

    ToSecondsBehavior(ValidDuration fixture) {
      durationFixture = fixture.duration();
      delayFixture = RetryDelay.of(fixture.duration());
    }

    @Test
    void shouldProvideExpectedSeconds() {
      assertThat(delayFixture.toSeconds()).isEqualTo(durationFixture.toSeconds());
    }

    @Test
    void shouldProvideExpectedSecondsAsInteger() {
      assertThat(delayFixture.toSecondsInt()).isEqualTo(Math.toIntExact(durationFixture.toSeconds()));
    }
  }

  @Nested
  @ParameterizedClass
  // Excluding milli-deviations as they cannot be distinguished based on
  // seconds representation.
  @EnumSource(
    value = ValidDuration.class,
    mode = EnumSource.Mode.EXCLUDE,
    names = {"MILLI_ABOVE_MIN_DELAY", "MILLI_BELOW_MAX_DELAY"}
  )
  class AsSecondsBehavior {
    private final Duration duration;

    AsSecondsBehavior(ValidDuration validDuration) {
      duration = validDuration.duration();
    }

    @Test
    void shouldDefaultToParseAsSecondsOnTrySaturatedParse() {
      long durationSeconds = duration.toSeconds();
      String durationAsString = Long.toString(durationSeconds);
      assertThat(RetryDelay.trySaturatedFromObject(durationAsString))
        .hasValue(RetryDelay.of(duration));
    }
  }

  @Nested
  class HumanReadableBehavior {
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
      retryDelay | expected
      60s        | 1m
      61s        | 1m1s
      1m         | 1m
      60m        | 1h
      61m        | 1h1m
      3601s      | 1h1s
      86398s     | 23h59m58s
      1d         | 1d
      """)
    // Resolving parameter RetryDelay from String requires only one static
    // factory method to exist, that returns RetryDelay and accepts String.
    // If this changes, a parameter parser must be added explicitly.
    void shouldProvideSomeHumanReadableDurationRepresentation(RetryDelay fixture,
                                                              String expectedString) {
      assertThat(fixture.humanReadable())
        .isEqualTo(expectedString);
    }
  }

  @Nested
  class TrySaturatedFromObjectBehavior {
    @Nested
    @ParameterizedClass
    @EnumSource(ValidDuration.class)
    class ValidDurationBehavior {
      private final Duration duration;

      ValidDurationBehavior(ValidDuration validDuration) {
        duration = validDuration.duration();
      }

      @ParameterizedTest
      @EnumSource(DurationFormat.Style.class)
      void shouldParseValidDurationStringToExpectedDelay(DurationFormat.Style durationFormatStyle) {
        String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
        assertThat(RetryDelay.trySaturatedFromObject(durationAsString))
          .hasValue(RetryDelay.of(duration));
      }

      @Test
      void shouldReturnRetryDelayAsIs() {
        RetryDelay retryDelay = RetryDelay.of(duration);
        assertThat(RetryDelay.trySaturatedFromObject(retryDelay))
          .containsSame(retryDelay);
      }

      @Test
      void shouldReturnDurationWrappedInRetryDelay() {
        assertThat(RetryDelay.trySaturatedFromObject(duration))
          .hasValue(RetryDelay.of(duration));
      }

      @Test
      void shouldReturnNumberAsRetryDelayInSeconds() {
        long seconds = duration.toSeconds();
        RetryDelay expected = RetryDelay.of(Duration.ofSeconds(seconds));
        assertThat(RetryDelay.trySaturatedFromObject(seconds))
          .hasValue(expected);
      }
    }

    @Nested
    @ParameterizedClass
    @EnumSource(InvalidDuration.class)
    class InvalidDurationBehavior {
      private final Duration duration;
      private final InvalidDuration invalidDuration;

      InvalidDurationBehavior(InvalidDuration invalidDuration) {
        this.invalidDuration = invalidDuration;
        duration = invalidDuration.duration();
      }

      @ParameterizedTest
      @EnumSource(DurationFormat.Style.class)
      void shouldParseDurationStringToBeWithinBounds(DurationFormat.Style durationFormatStyle) {
        // Excluding MIN, MAX, as they have issues in the String representation
        // for SIMPLE and COMPOSITE formatting, causing irrelevant failures.
        assumeThat(invalidDuration)
          .as("Format styles COMPOSITE AND SIMPLE have irrelevant issues with MIN, MAX fixtures. Ignoring these combinations.")
          .satisfiesAnyOf(
            id -> assertThat(id).isNotIn(InvalidDuration.MIN, InvalidDuration.MAX),
            id -> assertThat(durationFormatStyle).isNotIn(DurationFormat.Style.COMPOSITE, DurationFormat.Style.SIMPLE)
          );
        String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
        assertThat(RetryDelay.trySaturatedFromObject(durationAsString))
          .hasValueSatisfying(v -> assertThat(v).isBetween(RetryDelay.MIN_VALUE, RetryDelay.MAX_VALUE));
      }

      @Test
      void shouldReturnDurationToBeWithinBounds() {
        assertThat(RetryDelay.trySaturatedFromObject(duration))
          .hasValueSatisfying(v -> assertThat(v).isBetween(RetryDelay.MIN_VALUE, RetryDelay.MAX_VALUE));
      }

      @Test
      void shouldReturnNumberAsSecondsWithinBounds() {
        assertThat(RetryDelay.trySaturatedFromObject(duration.toSeconds()))
          .hasValueSatisfying(v -> assertThat(v).isBetween(RetryDelay.MIN_VALUE, RetryDelay.MAX_VALUE));
      }
    }

    @Nested
    @ParameterizedClass
    @ValueSource(strings = {
      // Empty string behavior most important here, as we expect it to trigger
      // using a default delay instead.
      "",
      // Test for accidental left-over space(s).
      " ",
      "lorem",
      "ipsum"
    })
    class InvalidDurationStringBehavior {
      private final String invalidDurationString;

      InvalidDurationStringBehavior(String invalidDurationString) {
        this.invalidDurationString = invalidDurationString;
      }

      @Test
      void shouldReturnEmptyOnInvalidDurationString() {
        assertThat(RetryDelay.trySaturatedFromObject(invalidDurationString))
          .isEmpty();
      }
    }
  }

  /*
   * ---------------------------------------------------------------------------
   * Test Fixtures
   * ---------------------------------------------------------------------------
   */

  enum ValidDuration {
    MIN_DELAY(RetryDelay.MIN_DELAY_DURATION),
    MILLI_ABOVE_MIN_DELAY(RetryDelay.MIN_DELAY_DURATION.plus(Duration.ofMillis(1L))),
    SECOND_ABOVE_MIN_DELAY(RetryDelay.MIN_DELAY_DURATION.plus(Duration.ofSeconds(1L))),
    FIVE_MINUTES(Duration.ofMinutes(5L)),
    ONE_HOUR(Duration.ofHours(1L)),
    DEFAULT_DELAY(RetryDelay.DEFAULT_DELAY_DURATION),
    SECOND_BELOW_MAX_DELAY(RetryDelay.MAX_DELAY_DURATION.minus(Duration.ofSeconds(1L))),
    MILLI_BELOW_MAX_DELAY(RetryDelay.MAX_DELAY_DURATION.minus(Duration.ofMillis(1L))),
    MAX_DELAY(RetryDelay.MAX_DELAY_DURATION),
    ;

    private final Duration duration;

    ValidDuration(Duration duration) {
      this.duration = duration;
    }

    public Duration duration() {
      return duration;
    }
  }

  enum InvalidDuration {
    MIN(Duration.ofSeconds(Long.MIN_VALUE)),
    ZERO(Duration.ZERO),
    MILLI_BELOW_MIN_DELAY(RetryDelay.MIN_DELAY_DURATION.minus(Duration.ofMillis(1L))),
    MILLI_ABOVE_MAX_DELAY(RetryDelay.MAX_DELAY_DURATION.plus(Duration.ofMillis(1L))),
    MAX(Duration.ofSeconds(Long.MAX_VALUE).plus(Duration.ofSeconds(1L).minus(Duration.ofNanos(1L)))),
    ;

    private final Duration duration;

    InvalidDuration(Duration duration) {
      this.duration = duration;
    }

    public Duration duration() {
      return duration;
    }
  }
}
