package com.coremedia.labs.translation.gcc.facade.mock;

import org.jspecify.annotations.NullMarked;

/**
 * Interface for a listener which will receive a signal when a
 * task state changed.
 */
@NullMarked
interface TaskStateListener {
  /**
   * Event receiver.
   *
   * @param task just the task which (already) changed its event
   */
  void taskStateChanged(Task task);
}
