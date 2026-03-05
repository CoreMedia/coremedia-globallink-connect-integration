package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings;
import org.gs4tr.gcc.restclient.GCExchange;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
 * This facade will mock the behavior of GCC. It is especially meant for
 * demo cases and for testing purpose.
 * <p>
 * To get an instance of this facade, use {@link GCExchangeFacadeSessionProvider}.
 */
@NullMarked
public final class MockedGCExchangeFacade implements GCExchangeFacade {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final ContentStore contentStore = new ContentStore();
  /**
   * The submission store to keep track of all submissions.
   * <p>
   * This must be a singleton instance, as it is used to keep track of all
   * submissions across multiple facade instances.
   */
  private static final SubmissionStore submissionStore = SubmissionStore.getInstance();
  private final MockSettings mockSettings;

  public MockedGCExchangeFacade(MockSettings mockSettings) {
    this.mockSettings = mockSettings;
    // By intention may adapt the settings also within the submission store
    // on each new instance creation of the facade. While this is not meant
    // to update the settings for existing/running submissions and tasks,
    // any update will be applied to new submissions and tasks.
    submissionStore.applySettings(mockSettings);
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
  public String uploadContent(String fileName, Resource resource, @Nullable Locale sourceLocale) {
    mockSettings.scenario().upload().startUpload();
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

    Optional<Integer> mockHttpResponse = mockSettings.scenario().cancellation().startCancellation();

    if (mockHttpResponse.isPresent()) {
      return mockHttpResponse.get();
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

    mockSettings.scenario().download().startDownload();

    boolean success = false;
    String untranslatedContent = task.getContent();
    String translatedContent = TranslationUtil.translateXliff(
      untranslatedContent,
      mockSettings.scenario().translate()
    );
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
    if (cancelledTasks.isEmpty()) {
      // There is no need to validate the actual state of the submission, as
      // from production code we can only reach `confirmCancelledTask` with
      // empty canceled tasks, when the submission is already in state
      // CANCELED.
      LOG.debug("Assuming cancellation got trigger from the (mocked) backend. Marking all tasks as canceled.");
      submissionStore.cancelSubmission(submissionId);
      cancelledTasks = submissionStore.getCancelledTasks(submissionId);
    }
    cancelledTasks.forEach(Task::markAsCancellationConfirmed);
  }


  @Override
  public GCSubmissionModel getSubmission(long submissionId) {
    // State: Will raise an exception if not found.
    GCSubmissionState submissionState = submissionStore.getSubmissionState(submissionId);

    return mockSettings.scenario().submission().intercept(
      GCSubmissionModel.builder(submissionId)
        .pdSubmissionIds(List.of(Long.toString(submissionId)))
        .state(submissionState)
        .build()
    );
  }
}
