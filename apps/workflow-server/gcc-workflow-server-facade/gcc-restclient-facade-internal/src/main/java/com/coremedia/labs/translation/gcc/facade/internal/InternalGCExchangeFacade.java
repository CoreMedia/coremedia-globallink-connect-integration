package com.coremedia.labs.translation.gcc.facade.internal;

import com.coremedia.blueprint.translation.TranslationService;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
public final class InternalGCExchangeFacade implements GCExchangeFacade {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final ContentStore contentStore = new ContentStore();
  private static final SubmissionStore submissionStore = new SubmissionStore();
  private TranslationService translationService;



  InternalGCExchangeFacade() {
  }

  public void setTranslationService(TranslationService translationService) {
    this.translationService = translationService;
  }

  /**
   * {@inheritDoc}
   *
   * @throws UnsupportedOperationException if called
   * @implNote Will throw an {@link UnsupportedOperationException} as there is no delegate available.
   */
  @Override
  public GCExchange getDelegate() {
    throw new UnsupportedOperationException("This facade does not provide a delegate.");
  }

  @Override
  public String uploadContent(String fileName, Resource resource, Locale sourceLocale) {
    return contentStore.addContent(resource);
  }

  @Override
  public long submitSubmission(@Nullable String subject, @Nullable String comment, ZonedDateTime dueDate, @Nullable String workflow, @Nullable String submitter, Locale sourceLocale, Map<String, List<Locale>> contentMap) {
    List<SubmissionContent> collect = contentMap.entrySet().stream()
            .map(e -> new SubmissionContent(
                    e.getKey(),
                    // Reads and removes the content.
                    contentStore.removeContent(e.getKey()),
                    e.getValue()))
            .collect(toList());
    return submissionStore.addSubmission(subject, collect);
  }

  @Override
  public int cancelSubmission(long submissionId) {
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
    boolean success = false;
    String untranslatedContent = task.getContent();
    String translatedContent = translationService.translateXliff(untranslatedContent, false).orElse(untranslatedContent);
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

  @Override
  public GCSubmissionModel getSubmission(long submissionId) {
    return new GCSubmissionModel(submissionId, Collections.singletonList(Long.toString(submissionId)), submissionStore.getSubmissionState(submissionId));
  }

}
