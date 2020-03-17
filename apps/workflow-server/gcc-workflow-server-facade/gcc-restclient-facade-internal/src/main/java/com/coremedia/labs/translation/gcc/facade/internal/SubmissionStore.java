package com.coremedia.labs.translation.gcc.facade.internal;

import com.coremedia.blueprint.translation.TranslationService;
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

  private static final AtomicLong NEXT_SUBMISSION_ID = new AtomicLong();
  private final Map<Long, Submission> submissions = new HashMap<>();

  long addSubmission(String subject, List<SubmissionContent> submissionContents) {
    Submission submission = new Submission(subject, submissionContents);
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
