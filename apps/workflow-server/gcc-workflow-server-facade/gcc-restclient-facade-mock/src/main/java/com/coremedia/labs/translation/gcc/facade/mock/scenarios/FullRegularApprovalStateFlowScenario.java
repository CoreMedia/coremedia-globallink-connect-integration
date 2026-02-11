package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A scenario which replays missing states according to the full regular approval
 * state flow of a submission.
 * <p>
 * The regular approval state flow is assumed to be:
 * <pre>
 * IN_PRE_PROCESS -&gt; STARTED -&gt; ANALYZED -&gt; AWAITING_APPROVAL -&gt; AWAITING_QUOTE_APPROVAL -&gt; TRANSLATE -&gt; REVIEW -&gt; COMPLETED
 * </pre>
 * <p>
 * States which interrupt any state flow are:
 * <pre>
 * CANCELLED, CANCELLATION_CONFIRMED
 * </pre>
 * <p>
 * This scenario enriches the standard mocked state flow, that skips several
 * states by intention, to a more complete state flow. This is especially
 * useful to test integrations which depend on certain states as well as the
 * proper localization of all states.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class FullRegularApprovalStateFlowScenario implements Scenario, SubmissionInterceptor {
  /**
   * Logger for this class.
   */
  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * ID of this scenario.
   */
  public static final String ID = "full-regular-approval-state-flow";

  /**
   * Enforced state flow. While these are replayed, we ignore the actual state
   * derived from tasks. The final delivered state will be reached
   * automatically derived from the task states, once the replay scenario
   * is done.
   */
  private static final List<GCSubmissionState> ENFORCED_STATE_FLOW = List.of(
    GCSubmissionState.IN_PRE_PROCESS,
    GCSubmissionState.STARTED,
    GCSubmissionState.ANALYZED,
    // Approval: The actual order of these states is not clear from state
    // diagrams. Any order helps us to test.
    GCSubmissionState.AWAITING_APPROVAL,
    GCSubmissionState.AWAITING_QUOTE_APPROVAL,
    GCSubmissionState.TRANSLATE,
    // Review: Unsure, where this is located. Not part of the regular GCC
    // documentation. The "Jobs List" API documentation provides a hint, that
    // it is correct to assume that review is after translation.
    GCSubmissionState.REVIEW,
    GCSubmissionState.COMPLETED
  );
  /**
   * States which interrupt any state flow.
   */
  private static final Set<GCSubmissionState> INTERRUPT_FLOW_STATES = EnumSet.of(
    GCSubmissionState.CANCELLED,
    GCSubmissionState.CANCELLATION_CONFIRMED
  );
  /**
   * Tracks submission state flows by submission ID.
   * <p>
   * Must be static to survive multiple facade instances.
   */
  private static final Map<Long, SubmissionStateFlow> SUBMISSION_STATE_FLOWS = new HashMap<>();

  @Override
  public String id() {
    return ID;
  }

  /**
   * Intercepts the given submission model and replays missing states according to
   * the full regular approval state flow.
   *
   * @param base the original submission model
   * @return the original or a modified submission model
   */
  @Override
  public GCSubmissionModel intercept(GCSubmissionModel base) {
    long submissionId = base.getSubmissionId();
    SubmissionStateFlow submissionStateFlow = getSubmissionStateFlow(submissionId);
    GCSubmissionState actualState = base.getState();
    GCSubmissionState replayedState = submissionStateFlow.replay(actualState);
    if (replayedState == actualState) {
      return base;
    }
    return GCSubmissionModel.builder(base)
      .state(replayedState)
      .build();
  }

  private static SubmissionStateFlow getSubmissionStateFlow(long submissionId) {
    synchronized (SUBMISSION_STATE_FLOWS) {
      return SUBMISSION_STATE_FLOWS.computeIfAbsent(submissionId, SubmissionStateFlow::new);
    }
  }

  /**
   * Tracks the state flow of a submission.
   */
  private static final class SubmissionStateFlow {
    /**
     * ID of the submission tracked by this state flow.
     */
    private final long submissionId;
    /**
     * Active replay states.
     */
    private final Deque<GCSubmissionState> activeReplayStates = new ArrayDeque<>();

    /**
     * Creates a submission state flow for the given submission ID.
     *
     * @param submissionId the submission ID
     */
    private SubmissionStateFlow(long submissionId) {
      this.submissionId = submissionId;
    }

    /**
     * Replays missing states according to the full regular approval state flow.
     *
     * @param actualState the actual state of the submission
     * @return the replayed state or the actual state, if no replay is done
     */
    private GCSubmissionState replay(GCSubmissionState actualState) {
      if (INTERRUPT_FLOW_STATES.contains(actualState)) {
        // Interrupt flow reached, nothing more to replay.
        activeReplayStates.clear();
        return actualState;
      }
      if (actualState == GCSubmissionState.STARTED) {
        activeReplayStates.addAll(ENFORCED_STATE_FLOW);
      }
      GCSubmissionState replayState = activeReplayStates.pollFirst();
      if (replayState != null) {
        boolean replayDone = activeReplayStates.isEmpty();
        LOG.info("Submission {}: Replaying state {} instead of actual state {} (replay done? {}).", submissionId, replayState, actualState, replayDone);
        return replayState;
      }
      return actualState;
    }

    @Override
    public String toString() {
      return "%s[activeReplayStates=%s, submissionId=%s]".formatted(lookup().lookupClass().getSimpleName(), activeReplayStates, submissionId);
    }
  }
}
