package com.coremedia.labs.translation.gcc.facade.mock.settings;

import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.findSubmissionStateByName;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * It is possible to mock a
 * {@link com.coremedia.labs.translation.gcc.facade.GCSubmissionState submission state}
 * to be reached before or after a given submission state has been reached.
 * Also, a given submission state can be replaced, so that instead of the
 * given state, another state is reached.
 * <p>
 * The goal is especially to test state progression, that is controlled via
 * the GCC backend, such as a possible state
 * {@link com.coremedia.labs.translation.gcc.facade.GCSubmissionState#REDELIVERED REDELIVERED}
 * after completing all tasks.
 * <p>
 * The configuration can be done in GlobalLink translation settings, with the
 * following structure (by example):
 * <pre>{@code
 * globalLink:
 *   mock:
 *     submissionStates:
 *       COMPLETED:
 *         after: REDELIVERED
 *         final: true
 *       DELIVERED:
 *         override:
 *           - OTHER
 *           - REDELIVERED
 *       REVIEW:
 *         before: OTHER
 * }</pre>
 * <p>
 * The generic structure is:
 * <pre>{@code
 * globalLink:
 *   mock:
 *     submissionStates:
 *       ACTUAL_SUBMISSION_STATE:
 *         before:
 *           - FIRST_SUBMISSION_STATE
 *           - SECOND_SUBMISSION_STATE
 *         after:
 *           - FIRST_SUBMISSION_STATE
 *           - SECOND_SUBMISSION_STATE
 *         override:
 *           - FIRST_SUBMISSION_STATE
 *           - SECOND_SUBMISSION_STATE
 *         final: boolean
 * }</pre>
 * <p>
 * All alternative states can be given as atomic string value or as array of
 * string values. In case of {@code before} and {@code after} the original state
 * will also be reached in general. In case of {@code override} the original
 * state will not be reached.
 * <p>
 * Without {@code final} after intercepting the state transitions, the normal
 * transitions will take over again. If {@code final} is set to {@code true},
 * as soon as no more manual transitions are available, the last state will be
 * reached and no further transitions will be possible.
 * <p>
 * As a special case, if only {@code final} is set to {@code true}, the adapted
 * state will never be left again.
 * <p>
 * Note, that once a mocked state replay is reached, this won't be intercepted
 * by other configurations. Thus, assuming that the state
 * {@link com.coremedia.labs.translation.gcc.facade.GCSubmissionState#OTHER OTHER}
 * can never be reached, the following configuration will just replace the state
 * {@link com.coremedia.labs.translation.gcc.facade.GCSubmissionState#COMPLETED COMPLETED}
 * by
 * {@link com.coremedia.labs.translation.gcc.facade.GCSubmissionState#OTHER OTHER}
 * and won't respect the mapping configuration for {@code OTHER}:
 * <pre>{@code
 * globalLink:
 *   mock:
 *     submissionState:
 *       COMPLETED:
 *         override: OTHER
 *       OTHER:
 *         override: DELIVERED
 * }</pre>
 */
@NullMarked
public final class MockSubmissionStates {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  public static final MockSubmissionStates EMPTY = new MockSubmissionStates(Map.of());

  private final Map<GCSubmissionState, StatePointcutConfig> statePointcutConfigs;

  /**
   * Creates a new mock submission state behavior.
   *
   * @param statePointcutConfigs map of state pointcut configurations
   */
  private MockSubmissionStates(Map<GCSubmissionState, StatePointcutConfig> statePointcutConfigs) {
    this.statePointcutConfigs = Map.copyOf(statePointcutConfigs);
  }

  /**
   * Parses a given configuration to mock submission state sequences.
   *
   * @param config configuration to parse
   * @return mock submission state behavior
   */
  public static MockSubmissionStates fromConfig(Map<?, ?> config) {
    Map<GCSubmissionState, StatePointcutConfig> parsedConfig = parseConfig(config);
    if (parsedConfig.isEmpty()) {
      return EMPTY;
    }
    return new MockSubmissionStates(parsedConfig);
  }

  /**
   * Parses a given configuration to a map of state pointcut
   * configurations.
   *
   * @param config configuration to parse
   * @return map of state pointcut configurations; on invalid or not existing
   * configuration an empty map will be returned
   */
  // jspecify-reference-checker: Fails to deal with instanceof pattern variable. Suppressed.
  @SuppressWarnings("nullness")
  private static Map<GCSubmissionState, StatePointcutConfig> parseConfig(Map<?, ?> config) {
    Map<GCSubmissionState, StatePointcutConfig> result = new EnumMap<>(GCSubmissionState.class);
    for (Map.Entry<?, ?> entry : config.entrySet()) {
      if (!(entry.getKey() instanceof String stateName)) {
        LOG.debug("Ignoring invalid type of state name: {}", entry.getKey());
        continue;
      }
      Optional<GCSubmissionState> state = findSubmissionStateByName(stateName);
      if (state.isEmpty()) {
        LOG.debug("Ignoring unknown submission state: {}", stateName);
        continue;
      }
      if (!(entry.getValue() instanceof Map<?, ?> stateConfig)) {
        LOG.debug("Ignoring invalid configuration for submission state {}: {}", stateName, entry.getValue());
        continue;
      }
      GCSubmissionState stateValue = state.get();
      Optional<StatePointcutConfig> pointcutConfig = StatePointcutConfig.fromConfig(stateConfig);
      pointcutConfig.ifPresent(configValue -> result.put(stateValue, configValue));
    }
    return result;
  }

  /**
   * Returns a replay scenario for a given submission state.
   *
   * @param state submission state to get a replay scenario for
   * @return replay scenario; {@code null} if no replay scenario is available
   */
  public @Nullable ReplayScenario getReplayScenario(GCSubmissionState state) {
    StatePointcutConfig config = statePointcutConfigs.get(state);
    if (config == null) {
      LOG.trace("No mock submission state configuration for state: {}", state);
      return null;
    }

    if (config.isEmpty()) {
      if (config.finalState()) {
        return new ReplayScenario(List.of(state), true);
      }
      return null;
    }

    List<GCSubmissionState> states = new ArrayList<>(config.before);

    if (config.override.isEmpty()) {
      states.add(state);
    } else {
      states.addAll(config.override);
    }

    states.addAll(config.after);

    return new ReplayScenario(states, config.finalState);
  }

  /**
   * Represents a replay scenario for a given submission state.
   */
  public static final class ReplayScenario {
    private final Deque<GCSubmissionState> states;
    private final boolean finalState;

    /**
     * Creates a new replay scenario.
     *
     * @param states     states to replay
     * @param finalState whether the last state is considered final
     */
    private ReplayScenario(Collection<GCSubmissionState> states,
                           boolean finalState) {
      this.states = new LinkedList<>(states);
      this.finalState = finalState;
    }

    /**
     * Number of states left.
     *
     * @return number of states left
     */
    public int size() {
      return states.size();
    }

    /**
     * Returns the next state to replay.
     *
     * @return next state to replay; empty if no more states are available
     */
    // jspecify-reference-checker: Fails to detect cannot-be-null for Optional.of() argument.
    @SuppressWarnings("nullness")
    public Optional<GCSubmissionState> next() {
      if (states.isEmpty()) {
        LOG.trace("No more states available in replay scenario.");
        return Optional.empty();
      }
      GCSubmissionState state;
      if (finalState && states.size() == 1) {
        // Stay at the last remaining state.
        state = states.peek();
      } else {
        state = states.poll();
      }
      LOG.trace("State to replay: {}", state);
      return Optional.of(state);
    }
  }

  /**
   * Configuration for a state pointcut.
   *
   * @param finalState whether the state is considered final
   * @param before     states to reach before the original state
   * @param after      states to reach after the original state
   * @param override   states to replace the original state
   */
  public record StatePointcutConfig(boolean finalState,
                                    List<GCSubmissionState> before,
                                    List<GCSubmissionState> after,
                                    List<GCSubmissionState> override
  ) {
    /**
     * Checks if the state configuration is considered empty.
     * <p>
     * It is considered empty, if no state transitions are configured.
     *
     * @return {@code true} if the state configuration is empty
     */
    public boolean isEmpty() {
      return before.isEmpty() && after.isEmpty() && override.isEmpty();
    }

    /**
     * Parses a given configuration to a state pointcut configuration.
     * <p>
     * The configuration may contain the keys {@code before}, {@code after},
     * {@code override} and {@code final}. All of them are optional. All
     * invalid values will be ignored.
     *
     * @param object configuration to parse
     * @return state pointcut configuration
     */
    // jspecify-reference-checker: Fails to deal with instanceof pattern variable. Suppressed.
    @SuppressWarnings("nullness")
    public static Optional<StatePointcutConfig> fromConfig(@Nullable Object object) {
      if (!(object instanceof Map<?, ?> config) || config.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(parseStateConfig(config));
    }

    /**
     * Parses a given configuration to a state pointcut configuration.
     * <p>
     * The configuration may contain the keys {@code before}, {@code after},
     * {@code override} and {@code final}. All of them are optional. All
     * invalid values will be ignored.
     *
     * @param config configuration to parse
     * @return state pointcut configuration
     */
    // jspecify-reference-checker: Fails to deal with instanceof pattern variable. Suppressed.
    @SuppressWarnings("nullness")
    private static StatePointcutConfig parseStateConfig(Map<?, ?> config) {
      List<GCSubmissionState> before = parseStateList(config.get("before"));
      List<GCSubmissionState> after = parseStateList(config.get("after"));
      List<GCSubmissionState> override = parseStateList(config.get("override"));
      boolean finalState = config.get("final") instanceof Boolean finalValue && finalValue;
      return new StatePointcutConfig(finalState, before, after, override);
    }

    /**
     * Parses a given object to a list of submission states (fault-tolerant).
     * <p>
     * If the object is a string, it will be parsed as a single state.
     * If the object is a list, all strings will be parsed as states, dropping
     * all non-string values as well as unknown states.
     *
     * @param config configuration to parse
     * @return list of submission states
     */
    // jspecify-reference-checker: Fails to deal with instanceof pattern variable. Suppressed.
    @SuppressWarnings("nullness")
    private static List<GCSubmissionState> parseStateList(@Nullable Object config) {
      if (config instanceof String stateName) {
        Optional<GCSubmissionState> state = findSubmissionStateByName(stateName);
        return state.map(List::of).orElse(List.of());
      }
      if (config instanceof List<?> list) {
        List<GCSubmissionState> states = new ArrayList<>();
        for (Object item : list) {
          if (item instanceof String stateName) {
            Optional<GCSubmissionState> state = findSubmissionStateByName(stateName);
            state.ifPresent(states::add);
          }
        }
        return states;
      }
      return List.of();
    }
  }
}
