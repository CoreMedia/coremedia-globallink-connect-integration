package com.coremedia.labs.translation.gcc.facade.mock;

import com.google.common.base.Splitter;
import org.jspecify.annotations.NullMarked;

import java.util.Arrays;

/**
 * Simple Task State Model.
 */
@NullMarked
enum TaskState {
  /**
   * Any other state, which we do not really care about. This is the initial
   * state of a task.
   */
  OTHER,
  /**
   * Task state is cancelled.
   */
  CANCELLED,
  /**
   * Task state is cancellation confirmed.
   */
  CANCELLATION_CONFIRMED,
  /**
   * Completed task (reached automatically).
   */
  COMPLETED,
  /**
   * Delivered task (reached manually).
   */
  DELIVERED;

  /**
   * Parse the given state string to a task-state. Defaults to {@link #OTHER}
   * if no matching task-state is found.
   *
   * @param taskStateString state string to parse; must exactly match task state name (no pre-processing applied to String, despite case)
   * @return parsed state; defaults to {@link #OTHER}
   */
  static TaskState parseState(String taskStateString) {
    return Arrays.stream(values())
            .filter(s -> s.name().equalsIgnoreCase(taskStateString))
            .findAny()
            .orElse(OTHER);
  }

  /**
   * Parse the states CSV to an array of {@link TaskState TaskStates}.
   * For convenience, case can be ignored and word separators might be
   * underscores, white spaces or dashes. CSV entries will be trimmed
   * before parsing.
   *
   * @param taskStatesCsv task states as comma-separated values; unmatched states will be mapped to task state {@link #OTHER}
   * @return array of task-states
   */
  static TaskState[] parseTaskStatesToArray(String taskStatesCsv) {
    return Splitter.on(',')
            .trimResults()
            .omitEmptyStrings()
            .splitToList(taskStatesCsv)
            .stream()
            // Some convenience replacements.
            .map(s -> s.replace(' ', '_'))
            .map(s -> s.replace('-', '_'))
            .map(TaskState::parseState)
            .toArray(TaskState[]::new);
  }
}
