package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * Model to store the data of a Task, returned by the GCC-Client.
 */
@SuppressWarnings("ClassCanBeRecord")
@NullMarked
public class GCTaskModel {
  private final long taskId;
  private final Locale taskLocale;

  public GCTaskModel(long taskId, Locale taskLocale) {
    this.taskId = taskId;
    this.taskLocale = taskLocale;
  }

  public long getTaskId() {
    return taskId;
  }

  public Locale getTaskLocale() {
    return taskLocale;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof GCTaskModel that)) {
      return false;
    }
    return taskId == that.taskId && Objects.equals(taskLocale, that.taskLocale);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskId, taskLocale);
  }

  @Override
  public String toString() {
    return String.valueOf(taskId);
  }
}
