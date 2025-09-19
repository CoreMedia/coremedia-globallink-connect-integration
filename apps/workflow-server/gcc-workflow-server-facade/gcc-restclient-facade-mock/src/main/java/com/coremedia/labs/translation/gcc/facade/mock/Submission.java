package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSubmissionStates;
import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

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
@NullMarked
final class Submission {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private final MockSettings mockSettings;
  private final ImmutableList<Task> tasks;

  /**
   * The active replay scenario, if any.
   * <p>
   * The tri-state logic is important:
   * <ul>
   *   <li>
   *     <strong>Unset</strong>:
   *     signals, that we have not checked yet, if for a given state a replay
   *     scenario is available.
   *   </li>
   *   <li>
   *     <strong>Set, but empty</strong>:
   *     signals, that we just replayed the states and there are no more states
   *     to replay. If queried again, we will return to the <strong>Unset</strong>
   *     state.
   *   </li>
   *   <li>
   *     <strong>Set and not empty</strong>:
   *     signals, that we have a replay scenario available, and we are in the
   *     process of replaying the states.
   *   </li>
   * </ul>
   */
  private MockSubmissionStates.@Nullable ReplayScenario activeReplayScenario;

  /**
   * <p>
   * Creates a submission. Note, that the state of the submission is actually controlled
   * via the tasks which will be created as part of this submission.
   * </p>
   * <p>
   * The subject will be used to parse, if it contains any task state switches to control
   * the behavior of the mocked submission transitions. To use the subject for state control
   * of the tasks it must only contain task state-control statements, prefixed with {@code states:}
   * followed by a list (separated by commas) of task-states as given by {@link TaskState}. Unmatched
   * strings will be mapped to {@link TaskState#OTHER}. For details on parsing have a look at
   * {@link TaskState#parseTaskStatesToArray(String)}.
   * </p>
   * <p>
   * If no task-states are given in {@code subject} the default behavior is, that tasks will
   * change to {@link TaskState#COMPLETED} after a time specified by {@code delayBaseSeconds}
   * and {@code delayOffsetPercentage}.
   * </p>
   *
   * @param subject            subject to provide possibility to control task switching
   * @param submissionContents contents which shall be part of the submission
   * @param mockSettings       settings to control the behavior of the submission
   */
  Submission(String subject,
             List<SubmissionContent> submissionContents,
             MockSettings mockSettings) {
    this.mockSettings = mockSettings;
    ImmutableList.Builder<Task> builder = ImmutableList.builder();
    for (SubmissionContent c : submissionContents) {
      for (int i = c.targetLocales().size(); i > 0; --i) {
        builder.add(new Task(
          c.fileContent(),
          mockSettings.stateChangeDelaySeconds(),
          mockSettings.stateChangeDelayOffsetPercentage(),
          c.targetLocales().get(i - 1),
          parseTaskStates(subject)
        ));
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

    return possiblyOverrideByMockSubmissionState(result);
  }

  private Optional<GCSubmissionState> getSubmissionStateFromReplayScenario() {
    MockSubmissionStates.ReplayScenario scenario = activeReplayScenario;
    if (scenario == null) {
      LOG.trace("No active replay scenario.");
      return Optional.empty();
    }
    Optional<GCSubmissionState> nextState = scenario.next();
    if (nextState.isEmpty()) {
      LOG.trace("Replay scenario exhausted. Resetting.");
      activeReplayScenario = null;
    } else {
      LOG.debug("Replay scenario (left: {}): {}", scenario.size(), nextState.get());
    }
    return nextState;
  }

  private GCSubmissionState possiblyOverrideByMockSubmissionState(GCSubmissionState originalSubmissionState) {
    if (activeReplayScenario != null) {
      // Prefer states from the replay scenario over the actual state.
      LOG.debug("Query existing replay scenario for state override of {}.", originalSubmissionState);
      return getSubmissionStateFromReplayScenario().orElse(originalSubmissionState);
    }
    activeReplayScenario = mockSettings.submissionStates().getReplayScenario(originalSubmissionState);
    if (activeReplayScenario != null) {
      LOG.debug("Query new replay scenario for state override of {}.", originalSubmissionState);
      return getSubmissionStateFromReplayScenario().orElse(originalSubmissionState);
    } else {
      LOG.trace("No replay scenario for state override of {}.", originalSubmissionState);
    }
    return originalSubmissionState;
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
    return new StringJoiner(", ", Submission.class.getSimpleName() + '[', "]")
      .add("state=" + getState())
      .add("tasks=" + tasks)
      .toString();
  }
}
