package com.coremedia.labs.translation.gcc.facade.mock;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Interface for listener which will receive a signal when a
 * task state changed.
 */
@DefaultAnnotation(NonNull.class)
interface TaskStateListener {
  /**
   * Event receiver.
   *
   * @param task just the task which (already) changed its event
   */
  void taskStateChanged(Task task);
}
