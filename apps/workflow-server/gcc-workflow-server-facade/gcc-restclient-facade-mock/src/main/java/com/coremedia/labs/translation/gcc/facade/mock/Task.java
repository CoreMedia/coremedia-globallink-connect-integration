package com.coremedia.labs.translation.gcc.facade.mock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Mock for a task. It has a very simple state-concept which just knows
 * completed, delivered and <em>other</em> (which is just everything else
 * we do not really care about).
 * </p>
 * <p>
 * The task automatically switches to <em>Completed</em> after some time. It
 * is then expected, that it gets set to <em>Delivered</em> eventually. All
 * state changes are signalled to the TaskStateListener, which by default is
 * the submission, which then again has the change to also change its state
 * according to all contained tasks.
 * </p>
 */
@NullMarked
final class Task {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final RandomGenerator RANDOM = new SecureRandom();
  private static final AtomicLong ID_FACTORY = new AtomicLong(System.currentTimeMillis());

  private final long id = ID_FACTORY.incrementAndGet();
  private final String content;
  private final Map<Long, TaskState> timeInMillisToState = new TreeMap<>();
  private final Locale targetLocale;
  private volatile boolean delivered;
  private volatile boolean cancelled;
  private volatile boolean cancellationConfirmed;

  /**
   * <p>
   * Creates a task for the given content. The task will automatically switch
   * states based on time. If no explicit task-states are defined that default
   * is to switch to state {@link TaskState#COMPLETED} after a given time
   * offset, controlled by {@code delayBaseSeconds} and {@code delayOffsetPercentage},
   * which gives some randomness to the generated offsets.
   * </p>
   * <p>
   * If more states are specified, exactly the given states will be reached,
   * i.e., if you don't add a completed state at the end it will not be added.
   * The time offsets will then be applied in between all task switches. Thus,
   * given two task-states the last state will be reached at about
   * {@code delayBaseSeconds * 2}.
   * </p>
   *
   * @param content               content represented as String (e.g., XLIFF)
   * @param delayBaseSeconds      base (minimum) offset in seconds
   * @param delayOffsetPercentage percentage offset to the base delay, which will either reduce or increase
   *                              the delay
   * @param targetLocale          targetLocale for the Task
   * @param taskStates            task states to reach based on timing; defaults to {@link TaskState#COMPLETED} if empty
   */
  Task(String content, long delayBaseSeconds, int delayOffsetPercentage, Locale targetLocale, TaskState... taskStates) {
    this.content = content;
    this.targetLocale = targetLocale;
    long currentTimeMillis = System.currentTimeMillis();
    if (taskStates.length == 0) {
      timeInMillisToState.put(currentTimeMillis + calcDelayMs(delayBaseSeconds, delayOffsetPercentage), TaskState.COMPLETED);
    } else {
      long taskSwitchTimeMillis = currentTimeMillis + calcDelayMs(delayBaseSeconds, delayOffsetPercentage);
      for (TaskState taskState : taskStates) {
        timeInMillisToState.put(taskSwitchTimeMillis, taskState);
        taskSwitchTimeMillis = taskSwitchTimeMillis + calcDelayMs(delayBaseSeconds, delayOffsetPercentage);
      }
    }
    LOG.info("Task State switch order (time in millis to state, current time: {}): {}", currentTimeMillis, timeInMillisToState);
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
    long currentTimeMillis = System.currentTimeMillis();
    TaskState result = TaskState.OTHER;
    for (Map.Entry<Long, TaskState> entry : timeInMillisToState.entrySet()) {
      long stateAtMillis = entry.getKey();
      if (currentTimeMillis >= stateAtMillis) {
        result = entry.getValue();
      }
    }
    syncTimeBasedStateWithStateFlags(result);
    return result;
  }

  public Locale getTargetLocale() {
    return targetLocale;
  }

  private void syncTimeBasedStateWithStateFlags(TaskState result) {
    switch (result) {
      case DELIVERED:
        delivered = true;
        break;
      case CANCELLED:
        cancelled = true;
        break;
      case CANCELLATION_CONFIRMED:
        cancelled = true;
        cancellationConfirmed = true;
        break;
      default:
        LOG.debug("State not persisted: {}", result);
    }
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
    return new StringJoiner(", ", Task.class.getSimpleName() + '[', "]")
            .add("currentTimeMillis=" + System.currentTimeMillis())
            .add("taskState=" + getTaskState())
            .add("timeInMillisToState=" + timeInMillisToState)
            .toString();
  }
}
