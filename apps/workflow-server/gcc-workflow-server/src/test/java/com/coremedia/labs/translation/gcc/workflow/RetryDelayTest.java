package com.coremedia.labs.translation.gcc.workflow;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryDelayTest {
  @Nested
  class ConstructorBehavior {
    @ParameterizedTest
    @EnumSource(ValidDuration.class)
    void shouldAcceptDurationsInRange(@NonNull ValidDuration fixture) {
      assertThatCode(() -> new RetryDelay(fixture.duration()))
        .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(InvalidDuration.class)
    void shouldDenyDurationOutOfBounds(@NonNull InvalidDuration fixture) {
      assertThatThrownBy(() -> new RetryDelay(fixture.duration()))
        .hasMessageContainingAll("value", "than or equal to")
        .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class OfBehavior {
    @ParameterizedTest
    @EnumSource(ValidDuration.class)
    void shouldAcceptDurationsInRange(@NonNull ValidDuration fixture) {
      assertThatCode(() -> RetryDelay.of(fixture.duration()))
        .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(InvalidDuration.class)
    void shouldDenyDurationOutOfBounds(@NonNull InvalidDuration fixture) {
      assertThatThrownBy(() -> RetryDelay.of(fixture.duration()))
        .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  class SaturatedOfBehavior {
    @ParameterizedTest
    @EnumSource(ValidDuration.class)
    void shouldAcceptDurationsInRange(@NonNull ValidDuration fixture) {
      assertThatCode(() -> RetryDelay.saturatedOf(fixture.duration()))
        .doesNotThrowAnyException();
    }

    @Nested
    @ParameterizedClass
    @EnumSource(InvalidDuration.class)
    class OutOfBoundsBehavior {
      @NonNull
      private final Duration durationFixture;

      OutOfBoundsBehavior(@NonNull InvalidDuration invalidDuration) {
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
    @NonNull
    private final RetryDelay delayFixture;
    @NonNull
    private final Duration durationFixture;

    ToSecondsBehavior(@NonNull ValidDuration fixture) {
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
  class ParseAndSaturatedParseBehavior {
    @Nested
    @ParameterizedClass
    @EnumSource(DurationFormat.Style.class)
    class AnyDurationFormatUnitBehavior {
      @NonNull
      private final DurationFormat.Style durationFormatStyle;

      AnyDurationFormatUnitBehavior(@NonNull DurationFormat.Style durationFormatStyle) {
        this.durationFormatStyle = durationFormatStyle;
      }

      @Nested
      @ParameterizedClass
      @EnumSource(ValidDuration.class)
      class ValidDurationBehavior {
        @NonNull
        private final Duration duration;

        ValidDurationBehavior(@NonNull ValidDuration validDuration) {
          duration = validDuration.duration();
        }

        @Test
        void shouldParseValidDurationToExpectedDelayViaParse() {
          String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
          assertThat(RetryDelay.parse(durationAsString))
            .isEqualTo(RetryDelay.of(duration));
        }

        @Test
        void shouldParseValidDurationToExpectedDelayViaSaturatedParse() {
          String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
          assertThat(RetryDelay.saturatedParse(durationAsString))
            .isEqualTo(RetryDelay.of(duration));
        }

        @Test
        void shouldParseValidDurationToExpectedDelayViaTrySaturatedParse() {
          String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
          assertThat(RetryDelay.trySaturatedParse(durationAsString))
            .hasValue(RetryDelay.of(duration));
        }
      }

      @Nested
      @ParameterizedClass
      // Excluding MIN, MAX, as they have issues in the String representation
      // for SIMPLE and COMPOSITE formatting, causing irrelevant failures.
      @EnumSource(
        value = InvalidDuration.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"MIN", "MAX"}
      )
      class InvalidDurationBehavior {
        @NonNull
        private final Duration duration;

        InvalidDurationBehavior(@NonNull InvalidDuration invalidDuration) {
          duration = invalidDuration.duration();
        }

        @Test
        void shouldDenyParsedDurationOutOfBoundsOnParse() {
          String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
          assertThatThrownBy(() -> RetryDelay.parse(durationAsString))
            .hasMessageContainingAll("value", "than or equal to")
            .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldNormalizeToWithinBoundsOnSaturatedParse() {
          String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
          assertThat(RetryDelay.saturatedParse(durationAsString))
            .isBetween(RetryDelay.MIN_VALUE, RetryDelay.MAX_VALUE);
        }

        @Test
        void shouldNormalizeToWithinBoundsOnTrySaturatedParse() {
          String durationAsString = DurationFormatterUtils.print(duration, durationFormatStyle);
          assertThat(RetryDelay.trySaturatedParse(durationAsString))
            .hasValueSatisfying(v -> assertThat(v).isBetween(RetryDelay.MIN_VALUE, RetryDelay.MAX_VALUE));
        }
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
      @NonNull
      private final Duration duration;

      AsSecondsBehavior(@NonNull ValidDuration validDuration) {
        duration = validDuration.duration();
      }

      @Test
      void shouldDefaultToParseAsSecondsOnParse() {
        long durationSeconds = duration.toSeconds();
        String durationAsString = Long.toString(durationSeconds);
        assertThat(RetryDelay.parse(durationAsString))
          .isEqualTo(RetryDelay.of(duration));
      }

      @Test
      void shouldDefaultToParseAsSecondsOnSaturatedParse() {
        long durationSeconds = duration.toSeconds();
        String durationAsString = Long.toString(durationSeconds);
        assertThat(RetryDelay.saturatedParse(durationAsString))
          .isEqualTo(RetryDelay.of(duration));
      }

      @Test
      void shouldDefaultToParseAsSecondsOnTrySaturatedParse() {
        long durationSeconds = duration.toSeconds();
        String durationAsString = Long.toString(durationSeconds);
        assertThat(RetryDelay.trySaturatedParse(durationAsString))
          .hasValue(RetryDelay.of(duration));
      }
    }

    @Nested
    @ParameterizedClass
    @ValueSource(strings = {"", "lorem", "ipsum"})
    class InvalidDurationStringBehavior {
      @NonNull
      private final String invalidDurationString;

      InvalidDurationStringBehavior(@NonNull String invalidDurationString) {
        this.invalidDurationString = invalidDurationString;
      }

      @Test
      void shouldDenyInvalidDurationStringOnParse() {
        assertThatThrownBy(() -> RetryDelay.parse(invalidDurationString))
          .isInstanceOf(IllegalArgumentException.class);
      }

      @Test
      void shouldDenyInvalidDurationStringOnSaturatedParse() {
        assertThatThrownBy(() -> RetryDelay.saturatedParse(invalidDurationString))
          .isInstanceOf(IllegalArgumentException.class);
      }

      @Test
      void shouldReturnEmptyOnInvalidDurationStringOnTrySaturatedParse() {
        assertThat(RetryDelay.trySaturatedParse(invalidDurationString))
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

    @NonNull
    private final Duration duration;

    ValidDuration(@NonNull Duration duration) {
      this.duration = duration;
    }

    @NonNull
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

    @NonNull
    private final Duration duration;

    InvalidDuration(@NonNull Duration duration) {
      this.duration = duration;
    }

    @NonNull
    public Duration duration() {
      return duration;
    }
  }
}
