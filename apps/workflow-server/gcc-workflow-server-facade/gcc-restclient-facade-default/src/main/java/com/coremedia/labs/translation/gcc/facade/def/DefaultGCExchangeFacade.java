package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeSessionProvider;
import com.coremedia.labs.translation.gcc.facade.GCFacadeAccessException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConnectorKeyConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeFileTypeConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeIOException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeSubmissionNotFoundException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.labs.translation.gcc.facade.config.GCSubmissionInstruction;
import com.coremedia.labs.translation.gcc.facade.config.GCSubmissionName;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import org.gs4tr.gcc.restclient.GCConfig;
import org.gs4tr.gcc.restclient.GCExchange;
import org.gs4tr.gcc.restclient.dto.GCResponse;
import org.gs4tr.gcc.restclient.dto.MessageResponse;
import org.gs4tr.gcc.restclient.dto.PageableResponseData;
import org.gs4tr.gcc.restclient.model.Connector;
import org.gs4tr.gcc.restclient.model.ContentLocales;
import org.gs4tr.gcc.restclient.model.GCSubmission;
import org.gs4tr.gcc.restclient.model.GCTask;
import org.gs4tr.gcc.restclient.model.TaskStatus;
import org.gs4tr.gcc.restclient.operation.SubmissionSubmit;
import org.gs4tr.gcc.restclient.operation.Submissions;
import org.gs4tr.gcc.restclient.operation.Tasks;
import org.gs4tr.gcc.restclient.request.SubmissionSubmitRequest;
import org.gs4tr.gcc.restclient.request.SubmissionsListRequest;
import org.gs4tr.gcc.restclient.request.TaskListRequest;
import org.gs4tr.gcc.restclient.request.UploadFileRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.gs4tr.gcc.restclient.model.TaskStatus.Cancelled;
import static org.gs4tr.gcc.restclient.model.TaskStatus.Completed;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * This is the default facade which directly interacts with the GCC REST
 * Backend via GCC Java RestClient API.
 * </p>
 * <p>
 * To create an instance of this facade, use {@link GCExchangeFacadeSessionProvider}.
 * </p>
 */
@NullMarked
public class DefaultGCExchangeFacade implements GCExchangeFacade {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final Integer HTTP_OK = 200;
  /**
   * Some string, so GCC can identify the source of requests.
   */
  private static final String USER_AGENT = lookup().lookupClass().getPackage().getName();

  private final Boolean isSendSubmitter;
  private final GCExchange delegate;
  private final Supplier<String> fileTypeSupplier;
  private final GCSubmissionName submissionName;
  private final GCSubmissionInstruction submissionInstruction;

  /**
   * Constructor.
   *
   * @param config configuration using keys as provided in {@link GCConfigProperty}.
   * @throws GCFacadeConfigException        if configuration is incomplete
   * @throws GCFacadeCommunicationException if connection to GCC failed.
   */
  DefaultGCExchangeFacade(Map<String, Object> config) {
    this(config, GCExchange::new);
  }

  @VisibleForTesting
  DefaultGCExchangeFacade(Map<String, Object> config,
                          Function<GCConfig, GCExchange> exchangeFactory) {
    String apiUrl = requireNonNullConfig(config, GCConfigProperty.KEY_URL);
    String connectorKey = requireNonNullConfig(config, GCConfigProperty.KEY_KEY);
    String apiKey = requireNonNullConfig(config, GCConfigProperty.KEY_API_KEY);
    isSendSubmitter = Boolean.valueOf(String.valueOf(config.get(GCConfigProperty.KEY_IS_SEND_SUBMITTER)));
    submissionName = GCSubmissionName.fromGlobalLinkConfig(config);
    submissionInstruction = GCSubmissionInstruction.fromGlobalLinkConfig(config);
    LOG.debug("Will connect to GCC endpoint: {}", apiUrl);
    try {
      GCConfig gcConfig = new GCConfig(apiUrl, apiKey);
      gcConfig.setUserAgent(USER_AGENT);
      gcConfig.setConnectorKey(connectorKey);
      // Redirect logging to SLF4j.
      gcConfig.setLogger(SLF4JHandler.getLogger(GCExchange.class));
      gcConfig.getLogger().finest("JUL Logging redirection to SLF4J: OK");
      LOG.trace("JUL Logging redirected to SLF4J.");
      delegate = exchangeFactory.apply(gcConfig);
      validateConnectorKey(delegate);
    } catch (GCFacadeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new GCFacadeCommunicationException(e, "Failed to connect to GCC at %s.", apiUrl);
    } catch (IllegalAccessError e) {
      throw new GCFacadeAccessException(e, "Cannot authenticate with API key.");
    }
    fileTypeSupplier = Suppliers.memoize(() -> {
      Object value = config.get(GCConfigProperty.KEY_FILE_TYPE);
      return getSupportedFileType(
          value == null ? null : String.valueOf(value));
      }
    );
  }

  @VisibleForTesting
  DefaultGCExchangeFacade(GCExchange delegate, String fileType) {
    this.delegate = delegate;
    fileTypeSupplier = () -> fileType;
    isSendSubmitter = false;
    submissionName = GCSubmissionName.DEFAULT;
    submissionInstruction = GCSubmissionInstruction.DEFAULT;
  }

  /**
   * GCC backend does not validate the connector key during connection setup.
   * Not validating it upfront may lead to unexpected states, such as that we
   * get just no submission for a given ID instead of any failure signal.
   * <p>
   * This validation is meant to fail-fast and to prevent unexpected, hard to
   * debug states.
   *
   * @param gcExchange GCC exchange instance to validate
   */
  private static void validateConnectorKey(GCExchange gcExchange) {
    String configuredKey = gcExchange.getConfig().getConnectorKey();
    List<String> availableConnectorKeys = gcExchange.getConnectors().stream().map(Connector::getConnectorKey).toList();
    if (!availableConnectorKeys.contains(configuredKey)) {
      throw new GCFacadeConnectorKeyConfigException("Connector key is unavailable in GCC (url=%s).".formatted(gcExchange.getConfig().getApiUrl()));
    }
  }

  private static String requireNonNullConfig(Map<String, Object> config, String key) {
    Object value = config.get(key);
    if (value == null) {
      throw new GCFacadeConfigException("Configuration for %s is missing. Configuration (values hidden): %s", key, config.entrySet().stream()
        .collect(toMap(Map.Entry::getKey, e -> GCConfigProperty.KEY_URL.equals(e.getKey()) ? e.getValue() : "*****"))
      );
    }
    return String.valueOf(value);
  }

  @Override
  public String uploadContent(String fileName, Resource resource, @Nullable Locale sourceLocale) {
    byte[] bytes;
    try (InputStream stream = resource.getInputStream()) {
      bytes = ByteStreams.toByteArray(stream);
    } catch (IOException e) {
      throw new GCFacadeIOException(e, "Failed to read resource: fileName=%s, resourceFileName=%s", fileName, resource.getFilename());
    }
    try {
      UploadFileRequest request = new UploadFileRequest(bytes, fileName, fileTypeSupplier.get());
      if (sourceLocale != null) {
        request.setSourceLocale(sourceLocale.toLanguageTag());
      }
      return delegate.uploadContent(request);
    } catch (GCFacadeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new GCFacadeCommunicationException(e, "Failed to upload content: %s", fileName);
    }
  }

  @Override
  public long submitSubmission(@Nullable String subject, @Nullable String comment, ZonedDateTime dueDate,
                               @Nullable String workflow, @Nullable String submitter, Locale sourceLocale,
                               Map<String, List<Locale>> contentMap) {

    List<ContentLocales> contentLocalesList = contentMap.entrySet().stream()
      .map(e ->
        new ContentLocales(
          e.getKey(),
          e.getValue().stream().map(Locale::toLanguageTag).collect(toList())))
      .collect(toList());

    SubmissionSubmitRequest request = new SubmissionSubmitRequest(
      createSubmissionName(subject, sourceLocale, contentMap),
      // REST API documents using UTC, Java REST Client API (v3.1.3)
      // uses local time zone instead. This may cause an
      // `IllegalArgumentException` if the due date is set to today with
      // only some hours offset.
      GCUtil.toUnixDateUtc(dueDate),
      sourceLocale.toLanguageTag(),
      contentLocalesList
    );
    if (comment != null) {
      // Expects incoming comments/notes are plain-text.
      String instructionsText = submissionInstruction.transformText(comment);
      request.setInstructions(instructionsText);
    }
    if (isSendSubmitter && submitter != null) {
      request.setSubmitter(submitter);
    }
    if (workflow != null) {
      request.setWorkflow(workflow);
    }

    try {
      SubmissionSubmit.SubmissionSubmitResponseData response = delegate.submitSubmission(request);
      return response.getSubmissionId();
    } catch (RuntimeException e) {
      throw new GCFacadeCommunicationException(e, "Failed to create submission: subject=%s, source-locale=%s",
        subject, sourceLocale.toLanguageTag());
    }
  }

  @Override
  public int cancelSubmission(long submissionId) {
    try {
      MessageResponse response = delegate.cancelSubmission(submissionId);
      if (LOG.isWarnEnabled() && !HTTP_OK.equals(response.getStatus())) {
        LOG.warn("Cannot cancel submission {}: {}", submissionId, gcResponseToString(response));
      }
      // MessageResponse has a statusCode (do not confuse with status), but
      // https://connect.translations.com/docs/api/GlobalLink_Connect_Cloud_API_Documentation.htm#submissions_cancel
      // does not document it. Since we cannot on-the-fly plug response#message
      // into Studio's resource bundles, the http result is the only useful data
      // here.
      return response.getStatus();
    } catch (RuntimeException e) {
      throw new GCFacadeCommunicationException(e, "Failed to cancel submission: id=" + submissionId);
    }
  }

  private static String gcResponseToString(GCResponse response) {
    return com.google.common.base.MoreObjects.toStringHelper(response)
      .add("status", response.getStatus())
      .add("statusCode", response.getStatusCode())
      .add("error", response.getError())
      .add("message", response.getMessage())
      .toString();
  }

  /**
   * Generates a submission name which shall be suitable for easily detecting
   * the submission in project director.
   *
   * @param subject      workflow subject
   * @param sourceLocale source locale
   * @param contentMap   content map, the target languages will be extracted from it
   * @return a descriptive string.
   */
  private String createSubmissionName(@Nullable String subject,
                                      Locale sourceLocale,
                                      Map<String, List<Locale>> contentMap) {
    String trimmedSubject = Objects.toString(subject, "").trim();
    String subjectWithDefault = trimmedSubject.isEmpty() ? Instant.now().toString() : trimmedSubject;
    String allTargetLocales = contentMap.entrySet().stream()
      .flatMap(e -> e.getValue().stream())
      .distinct()
      .map(Locale::toLanguageTag)
      .sorted()
      .collect(joining(", "));
    String withLocaleInfo = subjectWithDefault + " [" + sourceLocale.toLanguageTag() + " → " + allTargetLocales + ']';
    return submissionName.transform(withLocaleInfo);
  }

  @Override
  public void downloadCompletedTasks(long submissionId, BiPredicate<? super InputStream, ? super GCTaskModel> taskDataConsumer) {
    Map<TaskStatus, Set<GCTaskModel>> tasksByState = getTasksByState(submissionId, Completed);
    Set<GCTaskModel> completedTasks = tasksByState.getOrDefault(Completed, emptySet());

    LOG.debug("Completed Task IDs of submission {}: {}", submissionId, completedTasks);

    completedTasks.forEach(task -> downloadTask(task, taskDataConsumer));
  }

  @Override
  public void confirmCompletedTasks(long submissionId, Set<? super Locale> completedLocales) {
    Map<TaskStatus, Set<GCTaskModel>> tasksByState = getTasksByState(submissionId, Completed);
    Set<GCTaskModel> completedTasks = tasksByState.getOrDefault(Completed, emptySet());

    LOG.debug("Completed Task IDs of submission {}: {}", submissionId, completedTasks);

    for (GCTaskModel task : completedTasks) {
      try {
        completedLocales.add(task.getTaskLocale());
        delegate.confirmTask(task.getTaskId());
        LOG.debug("Confirmed delivery for the task {} of submission {}", task.getTaskId(), submissionId);
      } catch (RuntimeException e) {
        throw new GCFacadeCommunicationException(e, "Failed to confirm delivery for the task %s", task.getTaskId());
      }
    }
  }

  private void downloadTask(GCTaskModel task, BiPredicate<? super InputStream, ? super GCTaskModel> taskDataConsumer) {
    long taskId = task.getTaskId();
    try (InputStream is = delegate.downloadTask(taskId)) {
      if (taskDataConsumer.test(is, task)) {
        delegate.confirmTask(taskId);
      }
    } catch (IOException | RuntimeException e) {
      throw new GCFacadeCommunicationException(e, "Failed to download and confirm delivery for the task %s", taskId);
    }
  }

  @Override
  public void confirmCancelledTasks(long submissionId) {
    Map<TaskStatus, Set<GCTaskModel>> tasksByState =
      getTasksByState(submissionId,
        // Ignore tasks which got already confirmed as being cancelled.
        r -> r.setIsCancelConfirmed(0),
        Cancelled
      );
    List<GCTaskModel> tasks = new ArrayList<>(tasksByState.getOrDefault(Cancelled, emptySet()));

    List<Long> taskIds = tasks.stream()
      .map(GCTaskModel::getTaskId)
      .collect(toList());

    LOG.debug("Canceling Task IDs of submission {}: {}", submissionId, taskIds);
    confirmTaskCancellations(taskIds);
  }

  private void confirmTaskCancellations(List<Long> taskIds) {
    if (taskIds.isEmpty()) {
      // We must not send a request with empty list of task IDs.
      return;
    }

    for (Long taskId : taskIds) {
      try {
        MessageResponse messageResponse = delegate.confirmTaskCancellation(taskId);
        if (!HTTP_OK.equals(messageResponse.getStatus())) {
          LOG.debug("Failed to confirm task cancellation for the task {}. Will retry. Failed confirmation information: {}", taskId, messageResponse.getMessage());
          throw new GCFacadeCommunicationException("Failed to confirm the cancelled task " + taskId);
        }
      } catch (RuntimeException e) {
        throw new GCFacadeCommunicationException(e, "Failed to confirm the cancelled task: " + taskId);
      }
    }
  }

  /**
   * Retrieves all tasks of the given submission in the given states grouped
   * by their states.
   *
   * @param submissionId submission ID
   * @param taskStates   task states to include
   * @return map of task states to sets of {@link GCTaskModel}
   * @throws GCFacadeCommunicationException if tasks could be not be retrieved.
   */
  private Map<TaskStatus, Set<GCTaskModel>> getTasksByState(long submissionId, TaskStatus... taskStates) {
    return getTasksByState(submissionId, r -> {
    }, taskStates);
  }

  /**
   * Retrieves all tasks of the given submission in the given states grouped
   * by their states.
   *
   * @param submissionId        submission ID
   * @param requestPreProcessor pre-processor for request for further customization
   * @param taskStates          task states to include
   * @return map of task states to sets of {@link GCTaskModel}
   * @throws GCFacadeCommunicationException if tasks could be not be retrieved.
   */
  private Map<TaskStatus, Set<GCTaskModel>> getTasksByState(long submissionId,
                                                            Consumer<? super TaskListRequest> requestPreProcessor,
                                                            TaskStatus... taskStates) {
    Map<TaskStatus, Set<GCTaskModel>> tasksByState = new EnumMap<>(TaskStatus.class);

    GCUtil.processAllPages(
      () -> createTaskListRequestBase(submissionId, requestPreProcessor, taskStates),
      r -> executeRequest(r, tasksByState)
    );

    return tasksByState;
  }

  /**
   * Creates the base for the pageable {@code TaskListRequest}. This base will
   * be used to subsequently process through all pages.
   *
   * @param submissionId ID of the submission we want to retrieve the tasks for
   * @param taskStates   the task states which are relevant
   * @return base request.
   */
  private static TaskListRequest createTaskListRequestBase(long submissionId,
                                                           TaskStatus... taskStates) {
    return createTaskListRequestBase(submissionId, r -> {
    }, taskStates);
  }

  /**
   * Creates the base for the pageable {@code TaskListRequest}. This base will
   * be used to subsequently process through all pages.
   *
   * @param submissionId        ID of the submission we want to retrieve the tasks for
   * @param requestPreProcessor pre-processor for request for further customization
   * @param taskStates          the task states which are relevant
   * @return base request.
   */
  private static TaskListRequest createTaskListRequestBase(long submissionId,
                                                           Consumer<? super TaskListRequest> requestPreProcessor,
                                                           TaskStatus... taskStates) {
    TaskListRequest request = new TaskListRequest();
    request.setSubmissionId(submissionId);
    request.setTaskStatuses(taskStates);
    requestPreProcessor.accept(request);
    return request;
  }

  /**
   * Execute the {@code TaskListRequest} for one single page. Collect the results
   * into {@code tasksByState}.
   *
   * @param request      request to process
   * @param tasksByState result collector
   * @return {@code PageableResponseData} to retrieve the total number of pages.
   */
  private PageableResponseData executeRequest(TaskListRequest request, Map<TaskStatus, Set<GCTaskModel>> tasksByState) {
    return executeRequest(request, t ->
    {
      try {
        Locale localeFromGCCTask = new Locale.Builder().setLanguageTag(t.getTargetLocale().getLocale()).build();
        GCTaskModel gcTaskModel = new GCTaskModel(t.getTaskId(), localeFromGCCTask);
        tasksByState.merge(TaskStatus.valueOf(t.getState()), Sets.newHashSet(gcTaskModel),
          (oldValue, newValue) -> {
            oldValue.addAll(newValue);
            return oldValue;
          }
        );
      } catch (IllformedLocaleException exception) {
        LOG.error("Failed to convert LanguageTag tag from GCCTask with ID {}", t.getTaskId());
        throw exception;
      }

    });
  }

  /**
   * Execute the given task list response once (for one page) and forward the
   * retrieved tasks to the given consumer.
   *
   * @param request      request to process
   * @param taskConsumer result consumer
   * @return {@code PageableResponseData} to retrieve the total number of pages.
   */
  private PageableResponseData executeRequest(TaskListRequest request, Consumer<? super GCTask> taskConsumer) {
    Tasks.TasksResponseData taskData = delegate.getTasksList(request);

    if (taskData == null) {
      taskData = new Tasks.TasksResponseData();
    }

    List<GCTask> taskList = taskData.getTasks();

    if (taskList == null) {
      // Ensure non-null data
      taskData.setTasks(List.of());
    } else {
      taskList.forEach(taskConsumer);
    }

    return taskData;
  }

  @Override
  public GCSubmissionModel getSubmission(long submissionId) {
    GCSubmission submission = getSubmissionById(submissionId);
    if (submission == null) {
      throw new GCFacadeSubmissionNotFoundException("Submission not found for ID %d", submissionId);
    }
    GCSubmissionState state = GCSubmissionState.fromSubmissionState(submission.getStatus());
    // Fallback check on gcc-restclient update 2.4.0: Both ways to check for cancellation
    // seem to be appropriate.
    if (Boolean.TRUE.equals(submission.getIsCancelled()) || state == GCSubmissionState.CANCELLED) {
      /*
       * In order to know, that there is no more interaction with GCC backend
       * required to put the submission into a valid finished state, we split
       * the cancelled state into two. Only when the state is
       * "Cancellation Confirmed" there is nothing more to do. When it is
       * only "Cancelled" there are still tasks which need to be finished
       * either by confirming their cancellation or by downloading their
       * results.
       */
      if (areAllSubmissionTasksDone(submissionId)) {
        state = GCSubmissionState.CANCELLATION_CONFIRMED;
      } else {
        // Interpret the cancelled flag of submission as state. May be obsolete since gcc-restclient 2.4.0.
        state = GCSubmissionState.CANCELLED;
      }
    }
    return GCSubmissionModel.builder(submissionId)
      .pdSubmissionIds(submission.getPdSubmissionIds().keySet())
      .name(submission.getSubmissionName())
      .state(state)
      .submitter(submission.getSubmitter())
      .error(Boolean.TRUE.equals(submission.getIsError()))
      .build();
  }

  /**
   * All submission tasks are considered done if they are either
   * delivered, or their cancellation got confirmed.
   *
   * @param submissionId ID of submission
   * @return {@code true} if all tasks are considered done; {@code false} otherwise
   * @throws GCFacadeCommunicationException if the status could not be retrieved.
   */
  private boolean areAllSubmissionTasksDone(long submissionId) {
    AtomicBoolean allDone = new AtomicBoolean(true);
    GCUtil.processAllPages(
      () -> createTaskListRequestBase(submissionId),
      r -> executeRequest(r, t -> {
        TaskStatus status = TaskStatus.valueOf(t.getState());
        LOG.debug("Retrieved status \"{}\" of task {} of submission {}", status.text(), t.getTaskId(), submissionId);
        switch (status) {
          case Delivered:
            break;
          case Cancelled:
            LOG.debug("Verifying cancelation of task {} got confirmed -> {}", t.getTaskId(), t.getIsCancelConfirmed());
            // Logical AND: Only use confirmed state, if value
            // is still true. Otherwise, keep false state.
            allDone.compareAndSet(true, t.getIsCancelConfirmed());
            break;
          default:
            allDone.set(false);
        }
      })
    );

    return allDone.get();
  }

  /**
   * Retrieves the submission by ID.
   *
   * @param submissionId ID of the submission
   * @return submission found; {@code null} if not found
   * @throws GCFacadeCommunicationException if unable to retrieve.
   */
  private @Nullable GCSubmission getSubmissionById(long submissionId) {
    SubmissionsListRequest request = new SubmissionsListRequest();
    request.setSubmissionId(submissionId);
    request.setPageSize(1L);
    Submissions.SubmissionsResponseData responseData;
    try {
      responseData = delegate.getSubmissionsList(request);
    } catch (RuntimeException e) {
      throw new GCFacadeCommunicationException(e, "Failed to retrieve submission list for submission ID " + submissionId);
    }
    List<GCSubmission> submissions = responseData.getSubmissions();
    if (submissions == null || submissions.isEmpty()) {
      LOG.debug("Unable to find specified submission ID {}.", submissionId);
      return null;
    }
    if (responseData.getTotalResultPagesCount() > 1L) {
      LOG.warn(
        "More than one submission ({}) returned for the same ID {}. Will choose the first one.",
        responseData.getTotalResultPagesCount(),
        submissionId
      );
    }
    return submissions.get(0);
  }

  private String getSupportedFileType(@Nullable String configuredFileType) {
    String apiUrl = delegate.getConfig().getApiUrl();

    List<String> supportedFileTypes;
    try {
      supportedFileTypes = delegate.getConnectorsConfig().getFileTypes();
    } catch (RuntimeException e) {
      throw new GCFacadeCommunicationException(e, "Failed to get GlobalLink connector configuration from %s.", apiUrl);
    }

    if (supportedFileTypes == null || supportedFileTypes.isEmpty()) {
      throw new GCFacadeCommunicationException("No supported file types in GlobalLink connector config for %s", apiUrl);
    }

    String result;
    if (configuredFileType == null) {
      // if no file type is configured, just use the first from the list of supported file types
      result = supportedFileTypes.get(0);
    } else if (supportedFileTypes.contains(configuredFileType)) {
      // configured file type found in supported ones
      result = configuredFileType;
    } else {
      throw new GCFacadeFileTypeConfigException("Configured file type '%s' not in supported ones %s for GlobalLink " +
        "connection at %s", configuredFileType, supportedFileTypes, apiUrl);
    }

    LOG.info("Using file type '{}' for uploading data to GlobalLink at {}", result, apiUrl);
    return result;
  }

  @Override
  public GCExchange getDelegate() {
    return delegate;
  }
}
