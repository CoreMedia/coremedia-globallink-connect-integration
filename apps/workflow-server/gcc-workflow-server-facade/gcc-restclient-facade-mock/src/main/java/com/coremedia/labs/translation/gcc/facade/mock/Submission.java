package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.NullMarked;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Mock implementation of a submission. It has a very simplified state-model,
 * which is that it begins `Started`, will then change to `Complete` as soon as
 * all tasks are at least complete and will change to `Delivered` as soon as all
 * tasks are delivered.
 * <p>
 * This mock solution does not know of jobs: a submission has jobs (per locale),
 * which again have tasks (per content). This is because the current facade API
 * does not need to know of jobs but just of submissions and tasks.
 * <p>
 * As stated, that the default state flow of this submission is very simplified
 * only respecting typical breakpoints of a submission. This may be overridden
 * by the
 * {@link com.coremedia.labs.translation.gcc.facade.mock.scenarios.Scenario scenarios}.
 * <p>
 * In general, the state is automatically determined from the contained task
 * states, that will change to {@link TaskState#COMPLETED} after a time
 * specified by {@code delayBaseSeconds} and {@code delayOffsetPercentage}.
 */
@NullMarked
final class Submission {
  private final String subject;
  private final ImmutableList<Task> tasks;

  /**
   * Creates a submission. Note, that the state of the submission is actually controlled
   * via the tasks which will be created as part of this submission.
   *
   * @param subject            submission subject
   * @param submissionContents contents which shall be part of the submission
   * @param mockSettings       settings to control the behavior of the submission
   */
  Submission(String subject,
             List<SubmissionContent> submissionContents,
             MockSettings mockSettings) {
    this.subject = subject;
    ImmutableList.Builder<Task> builder = ImmutableList.builder();
    for (SubmissionContent c : submissionContents) {
      for (int i = c.targetLocales().size(); i > 0; --i) {
        builder.add(new Task(
          c.fileContent(),
          mockSettings.stateChangeDelaySeconds(),
          mockSettings.stateChangeDelayOffsetPercentage(),
          c.targetLocales().get(i - 1)
        ));
      }
    }
    tasks = builder.build();
  }

  GCSubmissionState getState() {
    GCSubmissionState result = GCSubmissionState.STARTED;

    if (atLeastOneTaskCancellationConfirmedRemainingDelivered()) {
      result = GCSubmissionState.CANCELLATION_CONFIRMED;
    } else if (anyTaskCancelled()) {
      result = GCSubmissionState.CANCELLED;
    } else if (allTasksDelivered()) {
      result = GCSubmissionState.DELIVERED;
    } else if (allTasksAtLeastCompleted()) {
      result = GCSubmissionState.COMPLETED;
    }

    return result;
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
    return taskStates().allMatch(s -> TaskState.COMPLETED == s || TaskState.DELIVERED == s);
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
      .filter(t -> taskState == t.getTaskState())
      .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return "%s[subject=%s, state=%s, tasks=%s]".formatted(MethodHandles.lookup().lookupClass().getSimpleName(), subject, getState(), tasks);
  }
}
