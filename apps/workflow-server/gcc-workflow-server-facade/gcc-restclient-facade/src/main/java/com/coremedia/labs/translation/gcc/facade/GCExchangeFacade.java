package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.gs4tr.gcc.restclient.GCExchange;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Facade for {@link org.gs4tr.gcc.restclient.GCExchange}.
 * <p>
 * Get instances of facades via a {@link GCExchangeFacadeSessionProvider}.
 *
 * @implSpec Implementations of this interface must be stateless, as they are
 * recreated several times during the lifecycle of the translation workflow.
 */
@DefaultAnnotation(NonNull.class)
public interface GCExchangeFacade {
  /**
   * This is a convenience access to the embedded delegate. It should not be
   * used within production code, but may help to develop faster and later
   * introduce required facades.
   *
   * @return delegate
   */
  @SuppressWarnings("unused")
  GCExchange getDelegate();

  /**
   * Uploads the given content, like for example an XLIFF file. Returns
   * the file ID which is later required in {@link #submitSubmission(String, String, ZonedDateTime, String, String, Locale, Map)}.
   *
   * @param fileName the filename of the resource
   * @param resource the resource to send
   * @param sourceLocale source locale (optional)
   * @return file ID to be used later on to submit the submission
   * @throws GCFacadeIOException            if the resource cannot be read
   * @throws GCFacadeCommunicationException if the file cannot be uploaded
   */
  String uploadContent(String fileName, Resource resource, Locale sourceLocale);

  /**
   * Submit submission for the given contents uploaded before.
   *
   * @param subject      workflow subject
   * @param comment      instructions for translators (optional)
   * @param dueDate      due date for the submission; implies the priority when translation jobs should be done
   * @param workflow     translation workflow to be used, if not the default (optional)
   * @param submitter    name of the submitter (optional)
   * @param sourceLocale source locale
   * @param contentMap   file IDs (returned by {@link #uploadContent(String, Resource, Locale)}) to translate
   *                     with the desired target locales to translate to
   * @return ID of the submission; to be used to track the state later on
   * @throws GCFacadeCommunicationException if submitting the submission failed
   * @see #uploadContent(String, Resource, Locale)
   */
  long submitSubmission(@Nullable String subject, @Nullable String comment, ZonedDateTime dueDate,
                        @Nullable String workflow, @Nullable String submitter, Locale sourceLocale,
                        Map<String, List<Locale>> contentMap) ;

  /**
   * Cancel a submission.
   *
   * @param submissionId the ID of the submission to cancel
   * @return The http result code of the underlying rest call
   */
  int cancelSubmission(long submissionId);

  /**
   * <p>
   * Downloads all completed tasks for the given submission ID. Downloaded
   * data are piped into {@code taskDataConsumer}.
   * </p>
   * <dl>
   * <dt><strong>TaskDataConsumer:</strong></dt>
   * <dd>
   * <p>
   * The {@code taskDataConsumer} has two important tasks:
   * </p>
   * <ul>
   * <li>apply the translation result to target contents (most likely XLIFF-import), and</li>
   * <li>signal, if it GCC backend shall be informed on successful delivery.</li>
   * </ul>
   * <p>
   * The {@code taskDataConsumer} must support two arguments:
   * </p>
   * <ul>
   * <li>An {@code InputStream}: The translation result</li>
   * <li>A {@code GCTaskModel}: The task, which may be useful for logging or other tracking purposes.</li>
   * </ul>
   * <p>
   * The {@code taskDataConsumer} must return {@code true} if and only if you never will try to download the corresponding
   * data again &mdash; in other words: if really everything was successful.
   * </p>
   * <p>
   * To signal a failure you may as well return {@code false} as you may raise a
   * {@link RuntimeException}. Both approaches will skip from marking the corresponding
   * data as delivered. Both approaches will not escalate the workflow (the exception
   * will just be logged, not rethrown).
   * </p>
   * </dd>
   * </dl>
   *
   * @param submissionId     ID of the submission to download completed task data of
   * @param taskDataConsumer consumer for the input data
   * @throws GCFacadeCommunicationException if completed tasks could not be downloaded or the {@code taskDataConsumer} threw an exception
   */
  void downloadCompletedTasks(long submissionId, BiPredicate<? super InputStream, ? super GCTaskModel> taskDataConsumer);

  /**
   * Confirms the download of all completed tasks without actually downloading their data. This method basically
   * ignores the result of completed translations.
   * Will also add the Locales of the completed Tasks to the given 'completedLocales' Set.
   *
   * <p>Use {@link #downloadCompletedTasks(long, BiPredicate)} instead to download the translation for all completed
   * tasks and confirm delivery, if successful.
   *
   * @param submissionId     ID of the submission to download completed task data of
   * @param completedLocales a Set of Locales where the Locales of the completed Tasks to confirm will be added to
   * @throws GCFacadeCommunicationException if the delivery for completed tasks could not be confirmed
   */
  void confirmCompletedTasks(long submissionId, Set<? super Locale> completedLocales);

  /**
   * <p>
   * Confirm all cancelled tasks of the given submission.
   * </p>
   *
   * @param submissionId ID of the submission
   * @throws GCFacadeCommunicationException if cancelled tasks could not be confirmed
   */
  void confirmCancelledTasks(long submissionId);

  /**
   * Get the submission model which contains information like its state
   *
   * @param submissionId ID of the submission
   * @return the submission model
   * @throws GCFacadeCommunicationException if the submission could not be retrieved
   */
  GCSubmissionModel getSubmission(long submissionId);
}
