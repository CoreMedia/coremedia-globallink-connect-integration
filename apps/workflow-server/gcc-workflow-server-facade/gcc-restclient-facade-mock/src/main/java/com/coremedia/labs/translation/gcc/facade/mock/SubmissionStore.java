package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
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
  private static final AtomicLong NEXT_SUBMISSION_ID = new AtomicLong();
  private final Map<Long, Submission> submissions = new HashMap<>();

  @NonNull
  private MockSettings mockSettings = MockSettings.EMPTY;

  /**
   * Lazy initialization of the singleton instance (Bill Pugh Singleton Design
   * Pattern).
   */
  private static class SingletonHelper {
    // The singleton instance is created when the SingletonHelper class is loaded
    private static final SubmissionStore INSTANCE = new SubmissionStore();
  }

  /**
   * Returns the singleton instance of the {@link SubmissionStore}.
   *
   * @return the singleton instance
   */
  public static SubmissionStore getInstance() {
    return SingletonHelper.INSTANCE;
  }

  /**
   * The private constructor to prevent instantiation.
   */
  private SubmissionStore() {
  }

  /**
   * Applies the given settings to the submission store. Note, that new settings
   * are to be expected to be applied to new submissions and tasks only.
   * @param mockSettings the settings to apply
   */
  public void applySettings(@NonNull MockSettings mockSettings) {
    this.mockSettings = mockSettings;
  }

  /**
   * Add a submission for the given target locales and the given contents.
   *
   * @param subject            subject may be used to control mocked task state switching; see {@link Submission#Submission(String, List, long, int)} for details.
   * @param submissionContents data (i.e., the XLIFF for example) to be translated
   * @return unique id for the submission
   */
  long addSubmission(String subject, List<SubmissionContent> submissionContents) {
    Submission submission = new Submission(subject, submissionContents, mockSettings.stateChangeDelaySeconds(), mockSettings.stateChangeDelayOffsetPercentage());
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
