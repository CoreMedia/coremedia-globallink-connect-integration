package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.CANCELLATION_CONFIRMED;
import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.CANCELLED;
import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.COMPLETED;
import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.DELIVERED;
import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.REDELIVERED;
import static com.coremedia.labs.translation.gcc.workflow.GlobalLinkWorkflowErrorCodes.SUBMISSION_CANCEL_FAILURE;
import static java.util.Objects.requireNonNull;

/**
 * Workflow action that cancels a GlobalLink submission.
 */
@NullMarked
public class CancelTranslationGlobalLinkAction extends
        GlobalLinkAction<CancelTranslationGlobalLinkAction.Parameters, CancelTranslationGlobalLinkAction.Result> {
  @Serial
  private static final long serialVersionUID = -4912724475227423848L;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String GCC_RETRY_DELAY_SETTINGS_KEY = "cancelTranslationRetryDelay";

  private static final int HTTP_OK = 200;

  private @Nullable String globalLinkSubmissionIdVariable;
  private @Nullable String globalLinkPdSubmissionIdsVariable;
  private @Nullable String globalLinkSubmissionStatusVariable;
  private @Nullable String cancelledVariable;
  private @Nullable String completedLocalesVariable;

  // --- construct and configure ------------------------------------

  public CancelTranslationGlobalLinkAction() {
    // Escalate in case of errors.
    // Some particular exceptions are handled in storeResult
    // and suppressed from escalation.
    super(true);
  }

  /**
   * Sets the name of the string process variable holding the ID of the translation submission.
   *
   * @param globalLinkSubmissionIdVariable string workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setGlobalLinkSubmissionIdVariable(String globalLinkSubmissionIdVariable) {
    this.globalLinkSubmissionIdVariable = requireNonNull(globalLinkSubmissionIdVariable);
  }

  /**
   * Sets the name of the process variable that holds the submission IDs shown to editors in Studio and
   * in the GlobalLink tools.
   *
   * @param globalLinkPdSubmissionIdsVariable name of workflow aggregation variable of type string
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setGlobalLinkPdSubmissionIdsVariable(String globalLinkPdSubmissionIdsVariable) {
    this.globalLinkPdSubmissionIdsVariable = requireNonNull(globalLinkPdSubmissionIdsVariable);
  }

  /**
   * Sets the name of the String process variable that represents the GlobalLink submission status
   *
   * @param globalLinkSubmissionStatusVariable boolean workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setGlobalLinkSubmissionStatusVariable(String globalLinkSubmissionStatusVariable) {
    this.globalLinkSubmissionStatusVariable = requireNonNull(globalLinkSubmissionStatusVariable);
  }

  /**
   * Sets the name of the boolean process variable to set to true if a cancel request was sent to the
   * translation service.
   *
   * @param cancelledVariable boolean workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCancelledVariable(String cancelledVariable) {
    this.cancelledVariable = cancelledVariable;
  }


  /**
   * Sets the name of the String process variable that represents the already translated locales
   *
   * @param completedLocalesVariable list workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCompletedLocalesVariable(String completedLocalesVariable) {
    this.completedLocalesVariable = completedLocalesVariable;
  }


  // --- GlobalLinkAction interface ----------------------------------------------------------------------

  @Override
  protected String getGCCRetryDelaySettingsKey() {
    return GCC_RETRY_DELAY_SETTINGS_KEY;
  }

  @Override
  Parameters doExtractParameters(Task task) {
    Process process = task.getContainingProcess();
    String submissionId = process.getString(globalLinkSubmissionIdVariable);
    boolean cancelled = process.getBoolean(cancelledVariable);
    Set<Locale> completedLocales = process.getStrings(completedLocalesVariable).stream()
            .map(Locale::forLanguageTag)
            .collect(Collectors.toCollection(HashSet::new));
    return new Parameters(parseSubmissionId(submissionId, task.getId()), cancelled, completedLocales);
  }

  // NullableProblems: IntelliJ IDEA 2025.2.2 notes false-positive "can be null". Ignored.
  @Override
  void doExecuteGlobalLinkAction(@SuppressWarnings("NullableProblems") Parameters params,
                                 Consumer<? super Result> resultConsumer,
                                 GCExchangeFacade facade,
                                 Map<String, List<Content>> issues) {
    requireNonNull(params, "Parameters must not be null.");
    long submissionId = params.submissionId;
    // Ignore Submission Error State: As we are trying to cancel the submission,
    // we don't care about the error state. At least for observed scenarios,
    // canceling an errored submission is the only way to get out of the error state.
    GCSubmissionModel submission = facade.getSubmission(submissionId);
    GCSubmissionState submissionState = submission.getState();
    boolean cancelled = params.cancelled;

    // Also store the PD submission ids — potentially they were not available before
    Result result = new Result(submissionState, cancelled, params.completedLocales, submission.getPdSubmissionIds());
    resultConsumer.accept(result);

    // nothing to do, if submission is already cancelled and confirmed or delivered
    if (submissionState == CANCELLATION_CONFIRMED
      || submissionState == DELIVERED
      || submissionState == REDELIVERED) {
      return;
    }

    // We cannot cancel a completed submission. The user may still have requested cancellation, for example
    // to resolve an error when importing the translation results. In that case, we just confirm the completed
    // submission, so that the submission will be marked as delivered. Set "cancelled" variable to true, so that
    // the workflow doesn't proceed with "ReviewDeliveredTranslation" when the submission is marked as delivered.
    if (submissionState == COMPLETED) {
      facade.confirmCompletedTasks(submissionId, result.completedLocales);
      result.submissionState = facade.getSubmission(submissionId).getState();
      LOG.info("Canceling completed submission {} (PD ID {}) with completed locales {} and new state {} is not allowed at GlobalLink. Confirming completion so that workflow can finish.",
              submission.getSubmissionId(), submission.getPdSubmissionIds(),
              result.completedLocales.stream().map(Locale::toLanguageTag).collect(Collectors.toList()),
              result.submissionState);
      result.cancelled = true;
      return;
    }

    // not yet cancelled -> cancel
    if (!cancelled && submissionState != CANCELLED) {
      result.cancelled = cancel(facade, submissionId, issues);
      result.submissionState = facade.getSubmission(submissionId).getState();
      if (result.cancelled) {
        LOG.info("Canceled submission {} (PD ID {}) with completed locales {} and new state {}.",
                submission.getSubmissionId(), submission.getPdSubmissionIds(),
                result.completedLocales.stream().map(Locale::toLanguageTag).collect(Collectors.toList()),
                result.submissionState);
      }
    }

    // cancelled but not yet confirmed -> confirm
    if (result.submissionState == CANCELLED) {
      facade.confirmCancelledTasks(submissionId);
      result.submissionState = facade.getSubmission(submissionId).getState();
    }
  }

  @Override
  @Nullable Void doStoreResult(Task task, Result result) {
    Process process = task.getContainingProcess();
    process.set(globalLinkSubmissionStatusVariable, result.submissionState.toString());
    process.set(cancelledVariable, result.cancelled);
    process.set(globalLinkPdSubmissionIdsVariable, result.pdSubmissionIds);

    List<String> completedLocalesStringList = result.completedLocales.stream()
            .map(Locale::toLanguageTag)
            .collect(Collectors.toList());
    process.set(completedLocalesVariable, completedLocalesStringList);
    return null;
  }

  // --- Internal ----------------------------------------------------------------------

  /**
   * Sends a cancel request to the GlobalLink service.
   *
   * @param facade       facade to use for communication
   * @param submissionId submission to cancel
   * @param issues       map to store issues during the cancel operation
   *                     (write-only)
   * @return {@code true} if the submission was successfully cancelled,
   * {@code false} otherwise
   */
  private static boolean cancel(GCExchangeFacade facade, long submissionId, Map<String, List<Content>> issues) {
    int httpStatus = facade.cancelSubmission(submissionId);
    if (httpStatus == HTTP_OK) {
      return true;

    }
    String errorCode = SUBMISSION_CANCEL_FAILURE;
    LOG.warn("Unable to cancel submission {}. Received status code: {} ({})", submissionId, httpStatus, errorCode);
    issues.put(errorCode, Collections.emptyList());
    return false;
  }

  record Parameters(long submissionId, boolean cancelled, Set<Locale> completedLocales) {
  }

  static class Result {
    GCSubmissionState submissionState;
    boolean cancelled;
    final Set<Locale> completedLocales;
    final List<String> pdSubmissionIds;

    Result(GCSubmissionState submissionState, boolean cancelled, Set<Locale> completedLocales, List<String> pdSubmissionIds) {
      this.submissionState = submissionState;
      this.cancelled = cancelled;
      this.completedLocales = completedLocales;
      this.pdSubmissionIds = pdSubmissionIds;
    }
  }
}
