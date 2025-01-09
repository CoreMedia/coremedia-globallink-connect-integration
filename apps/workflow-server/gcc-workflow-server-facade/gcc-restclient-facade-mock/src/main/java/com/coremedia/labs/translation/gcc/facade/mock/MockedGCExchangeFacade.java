package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockError;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSubmissionStates;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gs4tr.gcc.restclient.GCExchange;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * This facade will mock the behavior of GCC. It is especially meant for
 * demo cases and for testing purpose.
 * </p>
 * <p>
 * To get an instance of this facade, use {@link GCExchangeFacadeSessionProvider}.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
public final class MockedGCExchangeFacade implements GCExchangeFacade {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final ContentStore contentStore = new ContentStore();
  private static final SubmissionStore submissionStore = new SubmissionStore();
  @NonNull
  private final MockSettings mockSettings;
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
  @Nullable
  private MockSubmissionStates.ReplayScenario activeReplayScenario;

  MockedGCExchangeFacade(@NonNull MockSettings mockSettings) {
    this.mockSettings = mockSettings;
  }

  /**
   * @throws UnsupportedOperationException if called
   * @implNote Will throw an {@link UnsupportedOperationException} as there is no delegate available.
   */
  @Override
  public GCExchange getDelegate() {
    throw new UnsupportedOperationException("This facade does not provide a delegate.");
  }

  @Override
  public String uploadContent(String fileName, Resource resource, Locale sourceLocale) {
    if (mockSettings.error() == MockError.UPLOAD_COMMUNICATION) {
      throw new GCFacadeCommunicationException("Exception to test upload communication errors with translation service.");
    }
    return contentStore.addContent(resource);
  }

  @Override
  public long submitSubmission(@Nullable String subject, @Nullable String comment, ZonedDateTime dueDate, @Nullable String workflow, @Nullable String submitter, Locale sourceLocale, Map<String, List<Locale>> contentMap) {
    String trimmedSubject = Objects.toString(subject, "").trim();
    List<SubmissionContent> collect = contentMap.entrySet().stream()
            .map(e -> new SubmissionContent(
                    e.getKey(),
                    // Reads and removes the content.
                    contentStore.removeContent(e.getKey()),
                    e.getValue()))
            .collect(toList());
    return submissionStore.addSubmission(trimmedSubject, collect);
  }

  @Override
  public int cancelSubmission(long submissionId) {
    if (mockSettings.error() == MockError.CANCEL_COMMUNICATION) {
      throw new GCFacadeCommunicationException("Exception to test cancel communication errors with translation service.");
    }
    if (mockSettings.error() == MockError.CANCEL_RESULT) {
      // Any one of the possible errors documented in
      // https://connect-dev.translations.com/docs/api/v2/index.html#submissions_cancel
      // 400, 401, 404, 500
      return 404;
    }
    submissionStore.cancelSubmission(submissionId);
    return 200;  // http ok
  }

  @Override
  public void downloadCompletedTasks(long submissionId, BiPredicate<? super InputStream, ? super GCTaskModel> taskDataConsumer) {
    Collection<Task> completedTasks = submissionStore.getCompletedTasks(submissionId);
    completedTasks.forEach(t -> downloadTask(t, taskDataConsumer));
  }

  @Override
  public void confirmCompletedTasks(long submissionId, Set<? super Locale> completedLocales) {
    Collection<Task> completedTasks = submissionStore.getCompletedTasks(submissionId);
    for (Task completedTask : completedTasks) {
      completedLocales.add(completedTask.getTargetLocale());
      completedTask.markAsDelivered();
    }
  }

  private void downloadTask(Task task, BiPredicate<? super InputStream, ? super GCTaskModel> taskDataConsumer) {
    if (mockSettings.error() == MockError.DOWNLOAD_COMMUNICATION) {
      throw new GCFacadeCommunicationException("Exception to test download communication errors with translation service.");
    }
    boolean success = false;
    String untranslatedContent = task.getContent();
    String translatedContent = TranslationUtil.translateXliff(untranslatedContent, mockSettings.error() == MockError.DOWNLOAD_XLIFF);
    try (InputStream is = new ByteArrayInputStream(translatedContent.getBytes(StandardCharsets.UTF_8))) {
      success = taskDataConsumer.test(is, new GCTaskModel(task.getId(), task.getTargetLocale()));
    } catch (IOException e) {
      LOG.warn("Failed to read stream.", e);
    }
    if (success) {
      task.markAsDelivered();
    }
  }

  @Override
  public void confirmCancelledTasks(long submissionId) {
    Collection<Task> cancelledTasks = submissionStore.getCancelledTasks(submissionId);
    cancelledTasks.forEach(Task::markAsCancellationConfirmed);
  }

  @NonNull
  private Optional<GCSubmissionState> getSubmissionStateFromReplayScenario() {
    MockSubmissionStates.ReplayScenario scenario = activeReplayScenario;
    if (scenario == null) {
      return Optional.empty();
    }
    Optional<GCSubmissionState> nextState = activeReplayScenario.next();
    if (nextState.isEmpty()) {
      // Side-effect: Reset the active scenario.
      activeReplayScenario = null;
    }
    return nextState;
  }

  @NonNull
  private GCSubmissionState possiblyOverrideByMockSubmissionState(@NonNull GCSubmissionState originalSubmissionState) {
    if (activeReplayScenario != null) {
      // Prefer states from the replay scenario over the actual state.
      return getSubmissionStateFromReplayScenario().orElse(originalSubmissionState);
    }
    activeReplayScenario = mockSettings.submissionStates().getReplayScenario(originalSubmissionState);
    if (activeReplayScenario != null) {
      return getSubmissionStateFromReplayScenario().orElse(originalSubmissionState);
    }
    return originalSubmissionState;
  }

  @Override
  public GCSubmissionModel getSubmission(long submissionId) {
    GCSubmissionState originalSubmissionState = submissionStore.getSubmissionState(submissionId);
    GCSubmissionState actualSubmissionState = possiblyOverrideByMockSubmissionState(originalSubmissionState);
    return GCSubmissionModel.builder(submissionId)
            .pdSubmissionIds(List.of(Long.toString(submissionId)))
            .state(actualSubmissionState)
            .build();
  }
}
