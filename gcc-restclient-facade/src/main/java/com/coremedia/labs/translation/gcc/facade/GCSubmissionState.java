package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gs4tr.gcc.restclient.model.State;
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

  /**
   * Note: If you add or delete values of this Enum, also adapt the localization for Studio in the appurtenant file: 'GccProcessDefinitions.properties' (also for other languages)
   */

  IN_PRE_PROCESS(SubmissionStatus.InPreProcess),
  STARTED(SubmissionStatus.Started),
  ANALYZED(SubmissionStatus.Analyzed),
  AWAITING_APPROVAL(SubmissionStatus.AwaitingApproval),
  AWAITING_QUOTE_APPROVAL(SubmissionStatus.AwaitingQuoteApproval),
  IN_PROGRESS(SubmissionStatus.InProgress),
  /**
   * Workaround for translations-com/globallink-connect-cloud-api-java#1:
   * Submission state "Translate" is not yet available.
   *
   * @see <a href="https://github.com/translations-com/globallink-connect-cloud-api-java/issues/1">SubmissionStatus (model): Misses State "Translate" · Issue #1 · translations-com/globallink-connect-cloud-api-java</a>
   */
  TRANSLATE("Translate"),
  REVIEW(SubmissionStatus.Review),
  COMPLETED(SubmissionStatus.Completed),
  DELIVERED(SubmissionStatus.Delivered),
  /**
   * Artificial submission status for a cancelled submission. While the
   * GCC API keeps track of the cancellation state as flag rather
   * than as submission state, the actual state in which state a submission was
   * cancelled is not relevant to this API.
   */
  CANCELLED("Cancelled"),
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
    this.submissionStatusText = null;
  }

  GCSubmissionState(@NonNull SubmissionStatus submissionStatus) {
    this.submissionStatusText = submissionStatus.text();
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
  public static GCSubmissionState fromSubmissionState(@Nullable State submissionState) {
    // Note, that if queried for a submission state directly after it has been
    // started, may result in a submission state being 'null'.
    if (submissionState == null) {
      LOG.warn("Submission state unavailable (= null). Using OTHER as state.");
      return OTHER;
    }
    String submissionStateName = submissionState.getStateName();
    if (submissionStateName == null) {
      LOG.warn("Submission state name unavailable for: {} (state-number: {}). Using OTHER as state.", submissionState, submissionState.getStateNumber());
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
