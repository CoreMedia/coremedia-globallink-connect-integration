package com.coremedia.labs.translation.gcc.facade.mock;

import org.jspecify.annotations.NullMarked;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

import static java.lang.invoke.MethodHandles.lookup;

/**
 * Mock for a task. It has a very simple state-concept which just knows
 * completed, delivered and <em>other</em> (which is just everything else
 * we do not really care about).
 * <p>
 * The task automatically switches to <em>Completed</em> after some time. It
 * is then expected, that it gets set to <em>Delivered</em> eventually. All
 * state changes are signalled to the TaskStateListener, which by default is
 * the submission, which then again has the change to also change its state
 * according to all contained tasks.
 */
@NullMarked
public final class Task {
  private static final RandomGenerator RANDOM = new SecureRandom();
  private static final AtomicLong ID_FACTORY = new AtomicLong(System.currentTimeMillis());

  private final long id = ID_FACTORY.incrementAndGet();
  private final String content;
  private final Locale targetLocale;
  private final long autoCompletedAtMillis;
  private volatile boolean delivered;
  private volatile boolean cancelled;
  private volatile boolean cancellationConfirmed;

  /**
   * Creates a task for the given content. The task will automatically switch
   * to the state {@link TaskState#COMPLETED} after a given time offset,
   * controlled by {@code delayBaseSeconds} and {@code delayOffsetPercentage},
   * which gives some randomness to the generated offsets.
   *
   * @param content               content represented as String (e.g., XLIFF)
   * @param delayBaseSeconds      base (minimum) offset in seconds
   * @param delayOffsetPercentage percentage offset to the base delay, which will either reduce or increase
   *                              the delay
   * @param targetLocale          targetLocale for the Task
   */
  Task(String content, long delayBaseSeconds, int delayOffsetPercentage, Locale targetLocale) {
    this.content = content;
    this.targetLocale = targetLocale;
    autoCompletedAtMillis = System.currentTimeMillis() + calcDelayMs(delayBaseSeconds, delayOffsetPercentage);
  }

  private static long calcDelayMs(long delayBaseSeconds, int delayOffsetPercentage) {
    long baseMs = delayBaseSeconds * 1000L;
    if (delayOffsetPercentage <= 0) {
      return baseMs;
    }
    return baseMs + baseMs / 100L * (RANDOM.nextInt(2 * delayOffsetPercentage) - delayOffsetPercentage);
  }

  TaskState getTaskState() {
    if (cancellationConfirmed) {
      return TaskState.CANCELLATION_CONFIRMED;
    }
    if (cancelled) {
      return TaskState.CANCELLED;
    }
    if (delivered) {
      return TaskState.DELIVERED;
    }

    return getTaskStateByTimeOffset();
  }

  private TaskState getTaskStateByTimeOffset() {
    return System.currentTimeMillis() > autoCompletedAtMillis
      ? TaskState.COMPLETED
      : TaskState.OTHER;
  }

  public Locale getTargetLocale() {
    return targetLocale;
  }

  void markAsDelivered() {
    delivered = true;
  }

  void markAsCancelled() {
    if (!delivered) {
      cancelled = true;
    }
  }

  void markAsCancellationConfirmed() {
    if (!delivered) {
      cancellationConfirmed = true;
    }
  }

  String getContent() {
    return content;
  }

  long getId() {
    return id;
  }

  @Override
  public String toString() {
    return "%s[autoCompletedAtMillis=%s, cancellationConfirmed=%s, cancelled=%s, content=%s, delivered=%s, id=%s, targetLocale=%s, taskState=%s]".formatted(lookup().lookupClass().getSimpleName(), autoCompletedAtMillis, cancellationConfirmed, cancelled, content, delivered, id, targetLocale, getTaskState());
  }
}
