package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;

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
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GCTaskModel that = (GCTaskModel) o;
    return taskId == that.taskId &&
      Objects.equals(taskLocale, that.taskLocale);
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
