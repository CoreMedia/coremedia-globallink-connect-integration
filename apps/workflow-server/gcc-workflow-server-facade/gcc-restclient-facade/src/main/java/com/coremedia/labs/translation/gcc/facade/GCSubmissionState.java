package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gs4tr.gcc.restclient.model.Status;
import org.gs4tr.gcc.restclient.model.SubmissionStatus;
import org.slf4j.Logger;

import java.util.Arrays;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Convenience enum for {@link SubmissionStatus} which especially allows
 * to parse the String from a job list response for example.
 * </p>
 * <p>
 * While we may have decided to use {@link SubmissionStatus} directly, this
 * enum also provides a clear separation. As a result, it is recommended
 * that no other but this module have a dependencies to
 * GCC Java RestClient.
 * </p>
 *
 * @see SubmissionStatus
 */
@DefaultAnnotation(NonNull.class)
public enum GCSubmissionState {

  /*
   * ===========================================================================
   * Note: If you add or delete values of this Enum, also adapt the localization
   * for Studio in the appurtenant files: 'Gcc_properties.ts' and
   * 'GccWorkflowLocalization_properties.ts' (also for other languages)
   * ===========================================================================
   */

  /*
   * ===========================================================================
   * Hint: If you observe the GCC Rest Client not to provide a given state
   * observed in production scenarios, you may just add it here using the
   * received state name as string argument.
   * ===========================================================================
   */

  IN_PRE_PROCESS(SubmissionStatus.PreProcess),
  STARTED(SubmissionStatus.Started),
  ANALYZED(SubmissionStatus.Analyzed),
  AWAITING_APPROVAL(SubmissionStatus.AwaitingApproval),
  AWAITING_QUOTE_APPROVAL(SubmissionStatus.AwaitingQuoteApproval),
  /**
   * Signalled, that a translation is in progress.
   * @deprecated Removed in {@code gcc-restclient}. This state won't
   * be reached anymore.
   */
  @Deprecated(since = "gcc-restclient:2.4.0", forRemoval = true)
  IN_PROGRESS("In Progress"),
  TRANSLATE(SubmissionStatus.Translate),
  REVIEW(SubmissionStatus.Review),
  COMPLETED(SubmissionStatus.Completed),
  /**
   * State, observed to be reached in scenarios, where a submission
   * got manually set to redelivered, while the XLIFF has been sent
   * via other channels (like email).
   * <p>
   * The assumed behavior is, that a submission in state
   * {@link #COMPLETED completed} is set directly to redelivered, without
   * the XLIFF provided via the GCC backend.
   * <p>
   * The GCC API (v3.1.3) does not cover this state, so that we need to
   * mock it.
   *
   * @since 2406.1
   */
  REDELIVERED("Redelivered"),
  DELIVERED(SubmissionStatus.Delivered),
  CANCELLED(SubmissionStatus.Cancelled),
  /**
   * Artificial submission status for a cancelled submission completely
   * being marked as cancellation confirmed. In other words a submission
   * is considered to be in state <em>Cancellation Confirmed</em> when
   * the submission is cancelled and all of its tasks are either
   * cancelled (confirmed) or delivered.
   */
  CANCELLATION_CONFIRMED("Cancellation Confirmed"),
  /**
   * Artificial job state for any other state. Note, that this may signal
   * an unexpected GCC API change. You may also use this state if you
   * actually don't care about more details on the state.
   */
  OTHER();

  private static final Logger LOG = getLogger(lookup().lookupClass());

  @Nullable
  private final String submissionStatusText;

  GCSubmissionState() {
    submissionStatusText = null;
  }

  GCSubmissionState(@NonNull SubmissionStatus submissionStatus) {
    submissionStatusText = submissionStatus.text();
  }

  /**
   * This constructor is meant for translation states not yet available in
   * GCC RestClient API.
   *
   * @param submissionStatusText text
   */
  GCSubmissionState(@NonNull String submissionStatusText) {
    this.submissionStatusText = submissionStatusText;
  }

  /**
   * Transforms a submission state into a representation for the GCC facade.
   * On any failure/unknown state, {@link #OTHER} will be returned.
   *
   * @param submissionState GCC submission state to transform; {@code null} will always return {@link #OTHER}
   * @return representation for GCC facade
   */
  public static GCSubmissionState fromSubmissionState(@Nullable Status submissionState) {
    // Note, that if queried for a submission state directly after it has been
    // started, may result in a submission state being 'null'.
    if (submissionState == null) {
      LOG.warn("Submission state unavailable (= null). Using OTHER as state.");
      return OTHER;
    }
    String submissionStateName = submissionState.getStatusName();
    if (submissionStateName == null) {
      LOG.warn("Submission state name unavailable for: {} (state-number: {}). Using OTHER as state.", submissionState, submissionState.getStatusNumber());
      return OTHER;
    }
    return parseSubmissionStatusName(submissionStateName);
  }

  /**
   * Parse the status name and return the matching enum value. Empty, if
   * no status with the given name could be found.
   *
   * @param taskStatusName name to parse
   * @return status; {@link #OTHER} for any yet unknown status
   */
  private static GCSubmissionState parseSubmissionStatusName(String taskStatusName) {
    return Arrays.stream(values())
            .filter(s -> nonNull(s.submissionStatusText))
            .filter(s -> taskStatusName.equalsIgnoreCase(s.submissionStatusText))
            .findAny()
            .orElseGet(() -> {
              LOG.warn("Unknown submission state: {}. Using OTHER as state.", taskStatusName);
              return OTHER;
            });
  }
}
