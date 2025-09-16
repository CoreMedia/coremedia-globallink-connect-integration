package com.coremedia.labs.translation.gcc.util;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;

import java.time.Duration;
import java.util.Optional;

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
public record RetryDelay(@NonNull Duration value) implements Comparable<RetryDelay> {

  /**
   * Logger instance for this class.
   */
  @NonNull
  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * Minimum allowable delay duration.
   * <p>
   * Set to one minute to prevent DoS attacks on external systems.
   */
  @VisibleForTesting
  @NonNull
  static final Duration MIN_DELAY_DURATION = Duration.ofMinutes(1L);

  /**
   * Maximum allowable delay duration.
   * <p>
   * Set to one day to prevent excessively long workflow delays.
   */
  @VisibleForTesting
  @NonNull
  static final Duration MAX_DELAY_DURATION = Duration.ofDays(1L);

  /**
   * Default delay duration used as fallback for invalid values.
   */
  @VisibleForTesting
  @NonNull
  static final Duration DEFAULT_DELAY_DURATION = Duration.ofMinutes(15L);

  /**
   * Minimum delay between retrying communication with GlobalLink.
   * <p>
   * Firing too many update requests on the external system could be
   * considered a DoS attack.
   */
  @NonNull
  public static final RetryDelay MIN_VALUE = new RetryDelay(MIN_DELAY_DURATION);

  /**
   * Maximum delay between retrying communication with GlobalLink.
   * <p>
   * If the value is accidentally set to a very big delay, and the workflow
   * process picks this value, you will have to wait very long until it checks
   * again for an update. Changing this accidentally got also a lot more likely,
   * since times can be changed in the content repository directly.
   */
  @NonNull
  public static final RetryDelay MAX_VALUE = new RetryDelay(MAX_DELAY_DURATION);

  /**
   * Fallback delay between retrying communication with GlobalLink for illegal
   * values.
   */
  @NonNull
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
   * Compares this retry delay with another retry delay for order.
   *
   * @param o the retry delay to compare to
   * @return negative integer, zero, or positive integer as this delay
   * is less than, equal to, or greater than the specified delay
   * @throws NullPointerException if the specified delay is {@code null}
   */
  @Override
  public int compareTo(@NonNull RetryDelay o) {
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
  @NonNull
  public static RetryDelay saturatedOf(@NonNull Duration duration) {
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
  @NonNull
  private static String pretty(@NonNull Duration duration) {
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
  @NonNull
  public static RetryDelay saturatedParse(@NonNull String value) {
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
  @NonNull
  public static Optional<RetryDelay> findRetryDelay(@NonNull Object value) {
    requireNonNull(value);

    try {
      if (value instanceof RetryDelay) {
        return Optional.of((RetryDelay) value);
      }
      if (value instanceof Duration) {
        return Optional.of(saturatedOf((Duration) value));
      }
      if (value instanceof Number number) {
        return Optional.of(saturatedOf(Duration.ofSeconds(number.longValue())));
      }
      return Optional.of(saturatedParse(String.valueOf(value)));
    } catch (IllegalArgumentException e) {
      LOG.trace("Unable to parse retry delay value: {}. Returning empty.", value, e);
      return Optional.empty();
    }
  }
}
