package com.coremedia.labs.translation.gcc.facade.internal;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Mock implementation of a submission. It has a very simplified state-model,
 * which is that it begins `Started`, will then change to `Complete` as soon as
 * all tasks are at least complete and will change to `Delivered` as soon as all
 * tasks are delivered.
 * </p>
 * <p>
 * This mock solution does not know of jobs: a submission has jobs (per locale),
 * which again have tasks (per content). This is because the current facade API
 * does not need to know of jobs but just of submissions and tasks.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
final class Submission {

  private final ImmutableList<Task> tasks;

  Submission(String subject, List<SubmissionContent> submissionContents) {
    ImmutableList.Builder<Task> builder = ImmutableList.builder();
    for (SubmissionContent c : submissionContents) {
      for (int i = c.getTargetLocales().size(); i > 0; --i) {
        builder.add(new Task(c.getFileContent(), c.getTargetLocales().get(i - 1), parseTaskStates(subject)));
      }
    }
    tasks = builder.build();
  }

  private static TaskState[] parseTaskStates(String subject) {
    String lowerCaseSubject = subject.toLowerCase(Locale.ROOT);
    if (lowerCaseSubject.startsWith("states:")) {
      return TaskState.parseTaskStatesToArray(lowerCaseSubject.replace("states:", ""));
    }
    return new TaskState[0];
  }

  GCSubmissionState getState() {
    if (atLeastOneTaskCancellationConfirmedRemainingDelivered()) {
      return GCSubmissionState.CANCELLATION_CONFIRMED;
    }
    if (anyTaskCancelled()) {
      return GCSubmissionState.CANCELLED;
    }
    if (allTasksDelivered()) {
      return GCSubmissionState.DELIVERED;
    }
    if (allTasksAtLeastCompleted()) {
      return GCSubmissionState.COMPLETED;
    }
    return GCSubmissionState.STARTED;
  }

  void cancel() {
    tasks.forEach(Task::markAsCancelled);
  }

  private boolean anyTaskCancelled() {
    return taskStates().anyMatch(TaskState.CANCELLED::equals);
  }

  private boolean anyTaskCancellationConfirmed() {
    return taskStates().anyMatch(TaskState.CANCELLATION_CONFIRMED::equals);
  }

  private boolean atLeastOneTaskCancellationConfirmedRemainingDelivered() {
    if (anyTaskCancellationConfirmed()) {
      return taskStates().allMatch(((Predicate<TaskState>) TaskState.DELIVERED::equals).or(TaskState.CANCELLATION_CONFIRMED::equals));
    }
    return false;
  }

  private boolean allTasksDelivered() {
    return taskStates().allMatch(TaskState.DELIVERED::equals);
  }

  private boolean allTasksAtLeastCompleted() {
    return taskStates().allMatch(s -> TaskState.COMPLETED.equals(s) || TaskState.DELIVERED.equals(s));
  }

  private Stream<TaskState> taskStates() {
    return tasks.stream().map(Task::getTaskState);
  }

  Collection<Task> getCompletedTasks() {
    return getTasksInState(TaskState.COMPLETED);
  }

  Collection<Task> getCancelledTasks() {
    return getTasksInState(TaskState.CANCELLED);
  }

  private Collection<Task> getTasksInState(TaskState taskState) {
    return tasks.stream()
            .filter(t -> taskState.equals(t.getTaskState()))
            .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Submission.class.getSimpleName() + '[', "]")
            .add("state=" + getState())
            .add("tasks=" + tasks)
            .toString();
  }
}
