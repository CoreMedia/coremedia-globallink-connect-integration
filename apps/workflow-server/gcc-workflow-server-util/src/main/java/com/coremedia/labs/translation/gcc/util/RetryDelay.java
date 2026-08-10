package com.coremedia.labs.translation.gcc.util;

import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.format.datetime.standard.DurationFormatterUtils.detectAndParse;

/**
 * Represents a retry delay for GlobalLink actions.
 * <p>
 * This record enforces bounds checking to ensure retry delays are within
 * acceptable limits, preventing both DoS attacks from too-frequent requests
 * and excessively long delays that could stall workflow processes.
 *
 * @param value retry delay duration, must be between {@link #MIN_VALUE} and
 *              {@link #MAX_VALUE}
 * @since 2506.0.0-1
 */
@NullMarked
public record RetryDelay(Duration value) implements Comparable<RetryDelay> {

  /**
   * Logger instance for this class.
   */
  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * Minimum allowable delay duration.
   * <p>
   * Set to one minute to prevent DoS attacks on external systems.
   */
  @VisibleForTesting
  static final Duration MIN_DELAY_DURATION = Duration.ofMinutes(1L);

  /**
   * Maximum allowable delay duration.
   * <p>
   * Set to one day to prevent excessively long workflow delays.
   */
  @VisibleForTesting
  static final Duration MAX_DELAY_DURATION = Duration.ofDays(1L);

  /**
   * Default delay duration used as fallback for invalid values.
   */
  @VisibleForTesting
  static final Duration DEFAULT_DELAY_DURATION = Duration.ofMinutes(15L);

  /**
   * Minimum allowed jitter percentage, denoting no jitter at all, thus,
   * jitter being disabled.
   *
   * @since 2512.1.0-1
   */
  @VisibleForTesting
  static final int MIN_JITTER_PERCENTAGE = 0;

  /**
   * Maximum allowed jitter percentage, denoting a delay that may vary by
   * &plusmn;100&nbsp;% of its original value.
   *
   * @since 2512.1.0-1
   */
  @VisibleForTesting
  static final int MAX_JITTER_PERCENTAGE = 100;

  /**
   * Divisor to transform a percentage into its corresponding fraction.
   */
  private static final double PERCENTAGE_TO_FRACTION = 100.0d;

  /**
   * Minimum delay between retrying communication with GlobalLink.
   * <p>
   * Firing too many update requests on the external system could be
   * considered a DoS attack.
   */
  public static final RetryDelay MIN_VALUE = new RetryDelay(MIN_DELAY_DURATION);

  /**
   * Maximum delay between retrying communication with GlobalLink.
   * <p>
   * If the value is accidentally set to a very big delay, and the workflow
   * process picks this value, you will have to wait very long until it checks
   * again for an update. Changing this accidentally got also a lot more likely,
   * since times can be changed in the content repository directly.
   */
  public static final RetryDelay MAX_VALUE = new RetryDelay(MAX_DELAY_DURATION);

  /**
   * Fallback delay between retrying communication with GlobalLink for illegal
   * values.
   */
  public static final RetryDelay DEFAULT = new RetryDelay(DEFAULT_DELAY_DURATION);

  /**
   * Compact Constructor.
   * <p>
   * Validates that the provided duration is within acceptable bounds.
   *
   * @param value retry delay duration
   * @throws NullPointerException     if value is {@code null}
   * @throws IllegalArgumentException if value is outside permitted bounds
   */
  public RetryDelay {
    requireNonNull(value, "value must not be null");
    if (MIN_DELAY_DURATION.compareTo(value) > 0) {
      throw new IllegalArgumentException("value must be greater than or equal to %s".formatted(pretty(MIN_DELAY_DURATION)));
    }
    if (MAX_DELAY_DURATION.compareTo(value) < 0) {
      throw new IllegalArgumentException("value must be less than or equal to %s".formatted(pretty(MAX_DELAY_DURATION)));
    }
  }

  /**
   * Returns the value of the delay as seconds.
   *
   * @return number of seconds
   * @throws ArithmeticException if value exceeds {@code long} bounds
   */
  public long toSeconds() {
    return value.toSeconds();
  }

  /**
   * Returns the value of the delay as seconds.
   * <p>
   * This method should not throw {@code ArithmeticException} given our
   * current min and max values.
   *
   * @return number of seconds as integer
   * @throws ArithmeticException if value exceeds {@code int} bounds
   */
  public int toSecondsInt() {
    return Math.toIntExact(toSeconds());
  }

  /**
   * Returns a new retry delay with random jitter applied.
   * <p>
   * The resulting delay is chosen uniformly at random from
   * {@code [value * (1 - jitterFraction), value * (1 + jitterFraction)]} and
   * saturated into {@link #MIN_VALUE} to {@link #MAX_VALUE}. Applying jitter
   * helps to avoid that many workflow processes retry at the exact same
   * instant, for example, after a restart of the workflow server, which
   * otherwise may cause the external system to respond with
   * {@code HTTP 429 (Too Many Requests)}.
   * <p>
   * This method expects an already valid fraction. Use
   * {@link #findJitterFraction(Object)} to failure-safely parse and saturate
   * external configuration input into such a fraction.
   *
   * @param jitterFraction fraction to vary the delay by; {@code 0.0} is a
   *                       no-op, returning {@code this}, up to {@code 1.0},
   *                       denoting &plusmn;100&nbsp;%
   * @return a new, possibly jittered, retry delay
   * @throws IllegalArgumentException if jitterFraction is outside
   *                                  {@code [0.0, 1.0]}
   * @since 2512.1.0-1
   */
  public RetryDelay withJitter(double jitterFraction) {
    return withJitter(jitterFraction, ThreadLocalRandom.current());
  }

  /**
   * Returns a new retry delay with random jitter applied, using the given
   * random generator.
   * <p>
   * Same as {@link #withJitter(double)}, but with an injectable random source
   * to enable deterministic testing.
   *
   * @param jitterFraction fraction to vary the delay by
   * @param random         random generator to determine the actual jitter
   * @return a new, possibly jittered, retry delay
   * @throws IllegalArgumentException if jitterFraction is outside
   *                                  {@code [0.0, 1.0]}
   * @throws NullPointerException     if random is {@code null}
   * @since 2512.1.0-1
   */
  @VisibleForTesting
  RetryDelay withJitter(double jitterFraction, RandomGenerator random) {
    requireNonNull(random, "random must not be null");
    // Negated range check, to also reject NaN, which would otherwise pass
    // both individual bounds checks.
    if (!(jitterFraction >= 0.0d && jitterFraction <= 1.0d)) {
      throw new IllegalArgumentException("jitterFraction must be between 0.0 and 1.0: %s".formatted(jitterFraction));
    }
    // Exact comparison is intentional here: 0.0 is a documented sentinel for
    // "jitter disabled", not the result of a computation. Any value slightly
    // above 0.0 is handled correctly by the calculation below, so there is no
    // need for an epsilon-based comparison.
    if (jitterFraction == 0.0d) {
      return this;
    }
    double factor = 1.0d + random.nextDouble(-jitterFraction, jitterFraction);
    return saturatedOf(Duration.ofNanos(Math.round(value.toNanos() * factor)));
  }

  /**
   * Compares this retry delay with another retry delay for order.
   *
   * @param o the retry delay to compare to
   * @return negative integer, zero, or positive integer as this delay
   * is less than, equal to, or greater than the specified delay
   * @throws NullPointerException if the specified delay is {@code null}
   */
  @Override
  public int compareTo(RetryDelay o) {
    return value.compareTo(o.value);
  }

  /**
   * Returns the retry delay for the given duration unless it would overflow or
   * underflow in which case {@link #MAX_VALUE} or {@link #MIN_VALUE}
   * is returned, respectively.
   *
   * @param duration retry delay duration
   * @return retry delay; ensured to be within allowed bounds
   * @throws NullPointerException if duration is {@code null}
   */
  public static RetryDelay saturatedOf(Duration duration) {
    if (MIN_DELAY_DURATION.compareTo(duration) > 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Underflow of minimum retry delay duration: {}. Fallback to minimum retry delay.", pretty(duration));
      }
      return MIN_VALUE;
    }
    if (MAX_DELAY_DURATION.compareTo(duration) < 0) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Overflow of maximum retry delay duration: {}. Fallback to maximum retry delay.", pretty(duration));
      }
      return MAX_VALUE;
    }
    return new RetryDelay(duration);
  }

  /**
   * Provides a more human-readable representation of the given duration.
   * <p>
   * Uses Spring's {@code DurationFormatterUtils} with composite style formatting.
   * Falls back to {@code toString()} if composite formatting fails.
   *
   * @param duration duration to transform
   * @return human-readable representation
   * @throws NullPointerException if duration is {@code null}
   */
  private static String pretty(Duration duration) {
    try {
      return DurationFormatterUtils.print(duration, DurationFormat.Style.COMPOSITE);
    } catch (ArithmeticException e) {
      LOG.debug("Failed to pretty print as COMPOSITE: {}. Using default toString", duration, e);
      return duration.toString();
    }
  }

  /**
   * Parses the retry delay from the given value.
   * <p>
   * If the value represents a number its unit is expected to be seconds.
   * Alternative units may be given like {@code 15m}, {@code 1h}. For details
   * on duration parsing see
   * {@link DurationFormatterUtils#detectAndParse(String, DurationFormat.Unit)}.
   * <p>
   * Returns the result of the parsed value unless it would overflow or
   * underflow in which case {@link #MAX_VALUE} or {@link #MIN_VALUE}
   * is returned, respectively.
   *
   * @param value duration value to parse
   * @return parsed duration
   * @throws NullPointerException     if value is {@code null}
   * @throws IllegalArgumentException if value cannot be parsed
   */
  // Indirectly used from tests as parameter resolver.
  @VisibleForTesting
  static RetryDelay saturatedParse(String value) {
    return saturatedOf(detectAndParse(value, DurationFormat.Unit.SECONDS));
  }

  /**
   * Tries to determine the retry delay from the given value.
   * <p>
   * If the value represents a number its unit is expected to be seconds.
   * Alternative units may be given like {@code 15m}, {@code 1h}. For details
   * on duration parsing see
   * {@link DurationFormatterUtils#detectAndParse(String, DurationFormat.Unit)}.
   * <p>
   * Returns the result of the detected, transformed or parsed value unless it
   * would overflow or underflow in which case {@link #MAX_VALUE} or
   * {@link #MIN_VALUE} is returned, respectively.
   * <p>
   * Supported types of value:
   * <ul>
   * <li><strong>{@code RetryDelay}:</strong> returned as is</li>
   * <li><strong>{@code Number}:</strong> interpreted as seconds</li>
   * <li><strong>else:</strong> String representation will be parsed
   * as duration</li>
   * </ul>
   *
   * @param value duration value to detect, transform or parse
   * @return retry delay with detected duration, or empty if parsing fails
   * @throws NullPointerException if value is {@code null}
   */
  public static Optional<RetryDelay> findRetryDelay(Object value) {
    requireNonNull(value);

    try {
      return switch (value) {
        case RetryDelay retryDelay -> Optional.of(retryDelay);
        case Duration duration -> Optional.of(saturatedOf(duration));
        case Number number -> Optional.of(saturatedOf(Duration.ofSeconds(number.longValue())));
        default -> Optional.of(saturatedParse(String.valueOf(value)));
      };
    } catch (IllegalArgumentException e) {
      LOG.trace("Unable to parse retry delay value: {}. Returning empty.", value, e);
      return Optional.empty();
    }
  }

  /**
   * Tries to determine the jitter fraction from the given value.
   * <p>
   * The value is expected to represent an integer percentage, thus, a value
   * of {@code 20} denotes a jitter of &plusmn;20&nbsp;%. The result is the
   * corresponding fraction, ready to be passed to
   * {@link #withJitter(double)}.
   * <p>
   * Failure-safe, similar to {@link #findRetryDelay(Object)}:
   * <ul>
   * <li>A percentage outside {@link #MIN_JITTER_PERCENTAGE} to
   * {@link #MAX_JITTER_PERCENTAGE} is saturated into these bounds, just as
   * {@link #saturatedOf(Duration)} does for durations.</li>
   * <li>A value that cannot be interpreted as an integer at all results in an
   * empty {@code Optional}, which callers are expected to interpret as jitter
   * being disabled.</li>
   * </ul>
   * <p>
   * Supported types of value:
   * <ul>
   * <li><strong>{@code Number}:</strong> interpreted as percentage</li>
   * <li><strong>else:</strong> String representation will be parsed
   * as integer percentage</li>
   * </ul>
   *
   * @param value value to detect, transform or parse
   * @return jitter fraction, saturated to be within
   * {@code [0.0, 1.0]}; empty, if the value could not be interpreted as an
   * integer at all
   * @throws NullPointerException if value is {@code null}
   * @since 2512.1.0-1
   */
  public static Optional<Double> findJitterFraction(Object value) {
    requireNonNull(value);

    try {
      int percentage = switch (value) {
        case Number number -> number.intValue();
        default -> Integer.parseInt(String.valueOf(value).trim());
      };
      return Optional.of(saturatedJitterFraction(percentage));
    } catch (NumberFormatException e) {
      LOG.trace("Unable to parse jitter percentage value: {}. Returning empty.", value, e);
      return Optional.empty();
    }
  }

  /**
   * Transforms the given percentage into its corresponding fraction, saturated
   * to be within the allowed bounds.
   *
   * @param percentage percentage to transform
   * @return corresponding fraction, within {@code [0.0, 1.0]}
   */
  private static double saturatedJitterFraction(int percentage) {
    if (percentage < MIN_JITTER_PERCENTAGE) {
      LOG.debug("Underflow of minimum jitter percentage: {}. Fallback to minimum jitter percentage ({}, disabled).",
        percentage, MIN_JITTER_PERCENTAGE);
      return MIN_JITTER_PERCENTAGE / PERCENTAGE_TO_FRACTION;
    }
    if (percentage > MAX_JITTER_PERCENTAGE) {
      LOG.debug("Overflow of maximum jitter percentage: {}. Fallback to maximum jitter percentage ({}).",
        percentage, MAX_JITTER_PERCENTAGE);
      return MAX_JITTER_PERCENTAGE / PERCENTAGE_TO_FRACTION;
    }
    return percentage / PERCENTAGE_TO_FRACTION;
  }
}
