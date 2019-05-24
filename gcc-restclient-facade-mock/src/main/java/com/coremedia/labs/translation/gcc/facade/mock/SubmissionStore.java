package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Part of mocking the submission API. It will remember submissions, so that
 * you can later query their state.
 */
@DefaultAnnotation(NonNull.class)
final class SubmissionStore {
  private static final long DEFAULT_DELAY_BASE_SECONDS = 120L;
  private static final int DEFAULT_DELAY_OFFSET_PERCENTAGE = 50;

  private static final AtomicLong NEXT_SUBMISSION_ID = new AtomicLong();
  private final Map<Long, Submission> submissions = new HashMap<>();

  private long delayBaseSeconds = DEFAULT_DELAY_BASE_SECONDS;
  private int delayOffsetPercentage = DEFAULT_DELAY_OFFSET_PERCENTAGE;

  /**
   * Sets the state change delay for newly created tasks (base) in seconds.
   * Note, that it will get adapted by some random offset as configured by
   * {@link #setDelayOffsetPercentage(int)}.
   *
   * @param delayBaseSeconds delay in seconds
   */
  @VisibleForTesting
  void setDelayBaseSeconds(long delayBaseSeconds) {
    this.delayBaseSeconds = delayBaseSeconds;
  }

  /**
   * Random offset for state change delay for newly created tasks in percent.
   *
   * @param delayOffsetPercentage percentage (0 to 100) of the offset
   * @see #setDelayBaseSeconds(long)
   */
  @VisibleForTesting
  void setDelayOffsetPercentage(int delayOffsetPercentage) {
    if (delayOffsetPercentage < 0 || delayOffsetPercentage > 100) {
      throw new IllegalArgumentException("Offset Percentage must be between 0 and 100.");
    }
    this.delayOffsetPercentage = delayOffsetPercentage;
  }

  /**
   * Add a submission for the given target locales and the given contents.
   *
   * @param subject            subject may be used to control mocked task state switching; see {@link Submission#Submission(String, List, long, int)} for details.
   * @param submissionContents data (i. e. the XLIFF for example) to be translated
   * @return unique id for the submission
   */
  long addSubmission(String subject, List<SubmissionContent> submissionContents) {
    Submission submission = new Submission(subject, submissionContents, delayBaseSeconds, delayOffsetPercentage);
    long id = NEXT_SUBMISSION_ID.getAndIncrement();
    synchronized (submissions) {
      submissions.put(id, submission);
    }
    return id;
  }

  void cancelSubmission(long submissionId) {
    synchronized (submissions) {
      if (submissions.containsKey(submissionId)) {
        submissions.get(submissionId).cancel();
        return;
      }
    }
    throw new IllegalArgumentException("Unknown submission ID: " + submissionId);
  }

  GCSubmissionState getSubmissionState(long submissionId) {
    synchronized (submissions) {
      if (submissions.containsKey(submissionId)) {
        return submissions.get(submissionId).getState();
      }
    }
    throw new IllegalArgumentException("Unknown submission ID: " + submissionId);
  }

  Collection<Task> getCompletedTasks(long submissionId) {
    synchronized (submissions) {
      if (submissions.containsKey(submissionId)) {
        return submissions.get(submissionId).getCompletedTasks();
      }
    }
    throw new IllegalArgumentException("Unknown submission ID: " + submissionId);
  }

  Collection<Task> getCancelledTasks(long submissionId) {
    synchronized (submissions) {
      if (submissions.containsKey(submissionId)) {
        return submissions.get(submissionId).getCancelledTasks();
      }
    }
    throw new IllegalArgumentException("Unknown submission ID: " + submissionId);
  }
}
