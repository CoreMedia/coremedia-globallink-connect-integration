package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import static com.coremedia.labs.translation.gcc.workflow.GlobalLinkWorkflowErrorCodes.SUBMISSION_CANCEL_FAILURE;
import static java.util.Objects.requireNonNull;

/**
 * Workflow action that cancels a GlobalLink submission.
 */
public class CancelTranslationGlobalLinkAction extends
        GlobalLinkAction<CancelTranslationGlobalLinkAction.Parameters, CancelTranslationGlobalLinkAction.Result> {
  private static final long serialVersionUID = -4912724475227423848L;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int HTTP_OK = 200;

  private String globalLinkSubmissionIdVariable;
  private String globalLinkPdSubmissionIdsVariable;
  private String globalLinkSubmissionStatusVariable;
  private String cancelledVariable;
  private String completedLocalesVariable;

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
   * @param globalLinkPdSubmissionIdsVariable string workflow variable name
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
  Parameters doExtractParameters(Task task) {
    Process process = task.getContainingProcess();
    String submissionId = process.getString(globalLinkSubmissionIdVariable);
    boolean cancelled = process.getBoolean(cancelledVariable);
    Set<Locale> completedLocales = process.getStrings(completedLocalesVariable).stream()
            .map(Locale::forLanguageTag)
            .collect(Collectors.toCollection(HashSet::new));
    return new Parameters(parseSubmissionId(submissionId, task.getId()), cancelled, completedLocales);
  }

  @Override
  void doExecuteGlobalLinkAction(Parameters params, Consumer<? super Result> resultConsumer,
                                 GCExchangeFacade facade, Map<String, List<Content>> issues) {
    long submissionId = params.submissionId;
    GCSubmissionModel submission = facade.getSubmission(submissionId);
    GCSubmissionState submissionState = submission.getState();
    boolean cancelled = params.cancelled;

    // Also store the PD submission ids - potentially they were not available before
    Result result = new Result(submissionState, cancelled, params.completedLocales, submission.getPdSubmissionIds());
    resultConsumer.accept(result);

    // nothing to do, if submission is already cancelled and confirmed or delivered
    if (submissionState == CANCELLATION_CONFIRMED || submissionState == DELIVERED) {
      return;
    }

    // We cannot cancel a completed submission. The user may still have requested cancellation, for example
    // to resolve an error when importing the translation results. In that case, we just confirm the completed
    // submission, so that the submission will be marked as delivered. Set "cancelled" variable to true, so that
    // the workflow doesn't proceed with "ReviewDeliveredTranslation" when the submission is marked as delivered.
    if (submissionState == COMPLETED) {
      facade.confirmCompletedTasks(submissionId, result.completedLocales);
      result.submissionState = facade.getSubmission(submissionId).getState();
      result.cancelled = true;
      return;
    }

    // not yet cancelled -> cancel
    if (!cancelled && submissionState != CANCELLED) {
      result.cancelled = cancel(facade, submissionId, issues);
      result.submissionState = facade.getSubmission(submissionId).getState();
    }

    // cancelled but not yet confirmed -> confirm
    if (result.submissionState == CANCELLED) {
      facade.confirmCancelledTasks(submissionId);
      result.submissionState = facade.getSubmission(submissionId).getState();
    }
  }

  @Nullable
  @Override
  Void doStoreResult(Task task, Result result) {
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

  private static boolean cancel(GCExchangeFacade facade, long submissionId, Map<String, List<Content>> issues) {
    int httpStatus = facade.cancelSubmission(submissionId);
    if (httpStatus == HTTP_OK) {
      LOG.info("Cancelled submission {}", submissionId);
      return true;

    }
    String errorCode = SUBMISSION_CANCEL_FAILURE;
    LOG.warn("Unable to cancel submission {}. Received status code: {} ({})", submissionId, httpStatus, errorCode);
    issues.put(errorCode, Collections.emptyList());
    return false;
  }

  static class Parameters {
    final long submissionId;
    final boolean cancelled;
    final Set<Locale> completedLocales;

    Parameters(long submissionId, boolean cancelled, Set<Locale> completedLocales) {
      this.submissionId = submissionId;
      this.cancelled = cancelled;
      this.completedLocales = completedLocales;
    }
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
