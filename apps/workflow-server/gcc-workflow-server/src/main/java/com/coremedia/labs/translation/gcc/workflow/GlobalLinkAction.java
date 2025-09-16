package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.Blob;
import com.coremedia.cap.common.CapException;
import com.coremedia.cap.common.RelativeTimeLimit;
import com.coremedia.cap.common.RepositoryNotAvailableException;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.errorcodes.CapErrorCodes;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.translate.xliff.XliffImportResultCode;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.cap.workflow.plugin.ActionResult;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCFacadeAccessException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConnectorKeyConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeFileTypeConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeIOException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeSubmissionException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeSubmissionNotFoundException;
import com.coremedia.labs.translation.gcc.util.RetryDelay;
import com.coremedia.labs.translation.gcc.util.Settings;
import com.coremedia.labs.translation.gcc.util.SettingsSource;
import com.coremedia.rest.validation.Severity;
import com.coremedia.workflow.common.util.SpringAwareLongAction;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.UnknownNullness;
import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.coremedia.labs.translation.gcc.facade.DefaultGCExchangeFacadeSessionProvider.defaultFactory;
import static com.coremedia.labs.translation.gcc.util.Settings.GLOBAL_CONFIGURATION_PATH;
import static com.coremedia.labs.translation.gcc.util.Settings.SITE_CONFIGURATION_PATH;
import static java.util.Objects.requireNonNull;

/**
 * Abstract workflow {@link com.coremedia.cap.workflow.plugin.LongAction} that opens a connection to GlobalLink and
 * handles errors during action execution.
 *
 * <p>Concrete subclasses must implement
 * <ul>
 *   <li>{@link #doExtractParameters(Task)} to extract a value of type {@code <P>} from workflow variables that
 *   is passed as parameter to method {@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)}.
 *
 *   <li>{@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)} to execute the action. This method
 *   receives a parameter of type {@code <P>} that was returned by {@link #doExtractParameters(Task)} and sets the
 *   result value of type {@code <R>} at its consumer argument. The result value will then be passed to
 *   {@link #doStoreResult(Task, Object)}, which by default stores the value in the workflow variable that is
 *   configured in the workflow definition with attribute {@code resultVariable} at the {@code Action} element.
 *   Subclasses may also overwrite {@link #doStoreResult(Task, Object)} to store complex results in different
 *   workflow variables.
 * </ul>
 *
 * <p>Methods {@link #doExtractParameters(Task)}, {@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)}
 * and {@link #doStoreResult(Task, Object)} are called from {@link com.coremedia.cap.workflow.plugin.LongAction}
 * methods {@link com.coremedia.cap.workflow.plugin.LongAction#extractParameters(Task) extractParameters},
 * {@link com.coremedia.cap.workflow.plugin.LongAction#execute(Object) execute} and
 * {@link com.coremedia.cap.workflow.plugin.LongAction#storeResult(Task, Object) storeResult}, respectively,
 * and the constraints documented for those methods apply as well.
 *
 * @param <P> the type of the parameter passed to {@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)}
 *            that was previously returned by {@link #doExtractParameters(Task)}
 * @param <R> the type of the result value that {@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)}
 *            passes to its consumer argument and that is then passed as parameter to {@link #doStoreResult(Task, Object)}
 */
abstract class GlobalLinkAction<P, R> extends SpringAwareLongAction {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalLinkAction.class);

  @Serial
  private static final long serialVersionUID = -7130959823193680910L;

  /**
   * Name of the config parameter to control how many times communication
   * errors should be retried automatically. Defaults to {@link #DEFAULT_RETRY_COMMUNICATION_ERRORS} if unset.
   */
  private static final String CONFIG_RETRY_COMMUNICATION_ERRORS = "retryCommunicationErrors";
  private static final int DEFAULT_RETRY_COMMUNICATION_ERRORS = 5;

  private static final MimeType MIME_TYPE_JSON = mimeType("application/json");
  private static final Gson contentObjectReturnsIdGson = new GsonBuilder()
    .enableComplexMapKeySerialization()
    .registerTypeHierarchyAdapter(Content.class, new ContentObjectSerializer())
    .create();

  /**
   * Property for specification of delay in seconds before retrying Content
   * Management Server communication.
   * <p>
   * Values ensured to be within bounds:
   * {@code RetryDelay.MIN_VALUE} <= {@code value} <= {@code RetryDelay.MAX_VALUE}.
   * <p>
   * This value cannot be overwritten by the corresponding settings in the
   * content repository.
   */
  @VisibleForTesting
  static final String CMS_RETRY_DELAY_SETTINGS_KEY = "cms-retry-delay";

  /**
   * Property for specification of delay in seconds before retrying GlobalLink
   * communication.
   * <p>
   * Values ensured to be within bounds:
   * {@code RetryDelay.MIN_VALUE} <= {@code value} <= {@code RetryDelay.MAX_VALUE}.
   * <p>
   * This is only a fallback for sub-classing actions that don't provide a
   * unique retry delay by overwriting method
   * {@link #getGCCRetryDelaySettingsKey()}.
   */
  @VisibleForTesting
  static final String DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY = "gcc-retry-delay";

  private static final Set<String> REPOSITORY_UNAVAILABLE_ERROR_CODES = Set.of(
    CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE,
    CapErrorCodes.USER_REPOSITORY_UNAVAILABLE,
    CapErrorCodes.REPOSITORY_NOT_AVAILABLE
  );

  private String skipVariable;
  private String masterContentObjectsVariable;
  private String remainingAutomaticRetriesVariable;
  private String issuesVariable;
  private String retryDelayTimerVariable;

  // --- construct and configure ----------------------------------------------------------------------

  GlobalLinkAction(boolean rethrowResultException) {
    super(rethrowResultException);
  }

  /**
   * Sets the name of the boolean process variable which controls whether this action should be skipped.
   *
   * @param skipVariable variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setSkipVariable(String skipVariable) {
    this.skipVariable = skipVariable;
  }


  /**
   * Return the name of the process variable containing the source contents objects.
   *
   * @return the name of the process variable
   */
  String getMasterContentObjectsVariable() {
    return masterContentObjectsVariable;
  }

  /**
   * Set the name of the process variable containing the source contents objects.
   *
   * @param masterContentObjectsVariable the name of the process variable
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setMasterContentObjectsVariable(String masterContentObjectsVariable) {
    this.masterContentObjectsVariable = masterContentObjectsVariable;
  }

  /**
   * Sets the name of the Integer process variable that holds the number of remaining automatic retries after
   * errors that should be retried without user intervention.
   *
   * @param remainingAutomaticRetriesVariable integer workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setRemainingAutomaticRetriesVariable(String remainingAutomaticRetriesVariable) {
    this.remainingAutomaticRetriesVariable = remainingAutomaticRetriesVariable;
  }

  /**
   * Sets the name of the blob process variable to store a JSON blob with errors that happened when interacting
   * with the translation service, or null if no such errors occurred. Studio's TaskErrorValidator
   * can then display these errors as task issues. The JSON data structure is a serialized map from severity to
   * map of error code to list of affected contents, i.e. {@code Map<Severity, Map<String, List<Content>>>}.
   *
   * @param issuesVariable blob workflow variable name
   */
  @SuppressWarnings({"unused", "WeakerAccess"}) // set from workflow definition
  public void setIssuesVariable(String issuesVariable) {
    this.issuesVariable = requireNonNull(issuesVariable);
  }

  /**
   * The name of the Timer variable that will be initialized with value from the corresponding Spring properties or content
   * setting.
   *
   * @param retryDelayTimerVariable the name of the Timer variable
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setRetryDelayTimerVariable(String retryDelayTimerVariable) {
    this.retryDelayTimerVariable = retryDelayTimerVariable;
  }

  // --- LongAction interface ----------------------------------------------------------------------

  @Override
  public final Parameters<P> extractParameters(Task task) {
    Process process = task.getContainingProcess();

    if (skipVariable != null && process.getBoolean(skipVariable)) {
      return null;
    }

    List<ContentObject> masterContentObjects = process.getLinksAndVersions(getMasterContentObjectsVariable());
    Integer i = process.getInteger(remainingAutomaticRetriesVariable);
    int remainingAutomaticRetries = i != null ? i : 0;
    P extendedParameters = doExtractParameters(task);
    return new Parameters<>(extendedParameters, masterContentObjects, remainingAutomaticRetries);
  }

  /**
   * Returns the name of the setting to define the retry delay for GCC communication errors. Should be
   * overwritten by subclassing actions.
   */
  protected String getGCCRetryDelaySettingsKey() {
    return DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY;
  }

  /**
   * Utility method to retrieve a retry delay at a given key from settings.
   *
   * @param settings settings
   * @param key      settings key where to expect the delay to read and parse
   * @return parsed delay; a default when not available or failed parsing
   * the delay.
   */
  @NonNull
  private static RetryDelay getRetryDelay(@NonNull Settings settings,
                                          @NonNull String key) {
    return findRetryDelay(settings, key).orElse(RetryDelay.DEFAULT);
  }

  /**
   * Utility method to retrieve a retry delay at a given key from settings.
   *
   * @param settings settings
   * @param key      settings key where to expect the delay to read and parse
   * @return delay if found and valid; empty otherwise
   */
  @NonNull
  protected static Optional<RetryDelay> findRetryDelay(@NonNull Settings settings,
                                                       @NonNull String key) {
    return settings.at(key)
      .flatMap(RetryDelay::findRetryDelay);
  }

  @Override
  protected final Result<R> doExecute(Object params) {
    if (params == null) {
      // skip
      return null;
    }

    @SuppressWarnings("unchecked" /* per interface contract: params is the return value of #extractParameters */)
    Parameters<P> parameters = (Parameters<P>) params;

    Result<R> result = new Result<>();
    // maps error codes to affected contents; list of contents may be empty for some errors */
    Map<String, List<Content>> issues = new HashMap<>();

    RetryDelay retryDelay = RetryDelay.DEFAULT;
    int maxAutomaticRetries = 0; // if we ever get to this variable's usage, it will be set to something reasonable
    Settings settings = SettingsSource.fromContext(getSpringContext());

    try {
      settings = withGlobalSettings(settings, getConnection().getContentRepository());
      Site masterSite = getMasterSite(parameters.masterContentObjects);
      settings = withSiteSettings(settings, masterSite);
      retryDelay = getDefaultRetryDelay(settings);
      maxAutomaticRetries = maxAutomaticRetries(settings);

      GCExchangeFacade gccSession = openSession(settings);

      // call subclass implementation and store the result as result.extendedResult
      Consumer<R> resultConsumer = r -> result.extendedResult = Optional.of(r);
      doExecuteGlobalLinkAction(parameters.extendedParameters, resultConsumer, gccSession, issues);
    } catch (GCFacadeCommunicationException e) {
      // automatically retry upon communication errors until configured maximum of retries has been reached
      // but do not retry automatically if #doExecuteGlobalLinkAction returned additional issues
      return getResultForGCCConnectionError(e, result, issues, parameters, retryDelay, maxAutomaticRetries);
    } catch (GCFacadeSubmissionNotFoundException e) {
      LOG.warn("{}: Failed to find submission ({}).", getName(), GlobalLinkWorkflowErrorCodes.SUBMISSION_NOT_FOUND_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.SUBMISSION_NOT_FOUND_ERROR, List.of());
    } catch (GCFacadeSubmissionException e) {
      LOG.warn("{}: Failed to handle submission ({}).", getName(), GlobalLinkWorkflowErrorCodes.SUBMISSION_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.SUBMISSION_ERROR, List.of());
    } catch (GCFacadeIOException e) {
      LOG.warn("{}: Local I/O error ({}).", getName(), GlobalLinkWorkflowErrorCodes.LOCAL_IO_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.LOCAL_IO_ERROR, List.of());
    } catch (GCFacadeFileTypeConfigException e) {
      LOG.warn("{}: Communication failed because of unsupported configured file type ({})", getName(), GlobalLinkWorkflowErrorCodes.SETTINGS_FILE_TYPE_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.SETTINGS_FILE_TYPE_ERROR, List.of());
    } catch (GCFacadeConnectorKeyConfigException e) {
      LOG.warn("{}: Connector key is unavailable ({})", getName(), GlobalLinkWorkflowErrorCodes.SETTINGS_CONNECTOR_KEY_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.SETTINGS_CONNECTOR_KEY_ERROR, List.of());
    } catch (GCFacadeConfigException e) {
      LOG.warn("{}: Communication failed because of invalid/missing settings ({})", getName(), GlobalLinkWorkflowErrorCodes.SETTINGS_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.SETTINGS_ERROR, List.of());
    } catch (GCFacadeAccessException e) {
      LOG.warn("{}: Authentication with API key failed ({})", getName(), GlobalLinkWorkflowErrorCodes.INVALID_KEY_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.INVALID_KEY_ERROR, List.of());
    } catch (GCFacadeException e) {
      LOG.warn("{}: Unknown error occurred ({})", getName(), GlobalLinkWorkflowErrorCodes.UNKNOWN_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.UNKNOWN_ERROR, List.of());
    } catch (GlobalLinkWorkflowException e) {
      LOG.warn("{}: {} ({})", getName(), e.getMessage(), e.getErrorCode(), e);
      issues.put(e.getErrorCode(), List.of());
    } catch (RuntimeException e) {
      // automatically retry upon CMS connection errors
      return getResultForCMSConnectionError(settings, e, result);
    }

    // set retry delay for continuation of non-completed GlobalLink task, e.g.,
    // download of translations that are still missing
    result.retryDelaySeconds = adaptDelayForGeneralRetry(
      retryDelay,
      new AdaptDelayForGeneralRetryContext<>(
        settings,
        parameters.extendedParameters,
        result.extendedResult,
        issues
      )
    )
      .toSecondsInt();
    result.issues = issuesAsJsonBlob(issues);
    return result;
  }

  /**
   * Gets the default (general) retry delay to use.
   *
   * @param settings settings to read the delay from
   * @return delay to use
   */
  @NonNull
  private RetryDelay getDefaultRetryDelay(@NonNull Settings settings) {
    return getRetryDelay(settings, getGCCRetryDelaySettingsKey());
  }

  /**
   * Implementing actions may override this method, if they detect a state,
   * where the general retry delay should be adapted. Like, the action may
   * detect that it is in a state where the polling interval should be
   * decreased, thus, polling per time should be increased. Prominent example
   * here is the download action, where you may want to decrease the polling
   * interval until relevant information like the project director ID have been
   * retrieved.
   * <p>
   * This method gets a bunch of information passed, that is meant to assist to
   * decide if and how to adapt the retry delay.
   *
   * @param originalRetryDelay original (default/general) retry delay
   * @param context            some context you may want to respect for
   *                           adapting the delay
   * @return adapted (or unchanged) retry delay
   * @implSpec The default implementation returns the given delay as is.
   * @see #getRetryDelay(Settings, String)
   */
  @NonNull
  RetryDelay adaptDelayForGeneralRetry(@NonNull RetryDelay originalRetryDelay,
                                       @NonNull AdaptDelayForGeneralRetryContext<P, R> context) {
    return originalRetryDelay;
  }

  /**
   * Context information for {@link #adaptDelayForGeneralRetry(RetryDelay, AdaptDelayForGeneralRetryContext)}.
   *
   * @param settings           settings, like where to read an alternative delay from
   * @param extendedParameters your action specific parameters; result of
   *                           {@link #doExtractParameters(Task)}, thus,
   *                           the nullability state depends on the implementation
   * @param extendedResult     your action specific result
   * @param issues             a (mutable) map of issues; may be used to determine a
   *                           different behavior when issues exist as well as to add more
   *                           issues when problems arise adapting the retry delay
   * @param <P>                type of extended parameters
   * @param <R>                type of extended result
   */
  record AdaptDelayForGeneralRetryContext<P, R>(
    @NonNull Settings settings,
    @UnknownNullness P extendedParameters,
    @NonNull Optional<R> extendedResult,
    @NonNull Map<String, List<Content>> issues
  ) {
  }

  @Override
  public final ActionResult storeResult(Task task, Object result) {
    checkNotAborted(task);
    if (result instanceof Exception) {
      return storeResultException(task, (Exception) result);
    }
    if (result == null) {
      // skip
      return ActionResult.SUCCESSFUL;
    }

    @SuppressWarnings("unchecked" /* per interface contract: result is the return value of #doExecute */)
    Result<R> r = (Result<R>) result;

    Process process = task.getContainingProcess();
    process.set(remainingAutomaticRetriesVariable, r.remainingAutomaticRetries);
    // check for existence of variable to support backwards compatibility with old workflow definitions
    if (retryDelayTimerVariable != null) {
      process.set(retryDelayTimerVariable, new RelativeTimeLimit(r.retryDelaySeconds));
    }
    process.set(issuesVariable, r.issues);

    Object resultValue = r.extendedResult
      .map(extendedResult -> doStoreResult(task, extendedResult))
      .orElse(null);
    return super.storeResult(task, resultValue);
  }

  // --- Methods to be implemented / overridden by concrete subclass -------------------------------

  /**
   * Extract parameters from the {@link Task} and return them as a single
   * object of type {@code <T>}. The result will be passed as argument to method
   * {@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)}.
   * <p>
   * This method is called from
   * {@link com.coremedia.cap.workflow.plugin.LongAction#extractParameters(Task)}
   * and the constraints documented for that method apply here as well.
   *
   * @param task the task in which the action should be executed
   * @return the parameters for the actual computation or {@code null} if no
   * parameters are needed
   */
  @Nullable
  abstract P doExtractParameters(Task task);

  /**
   * Executes the action and optionally sets a result value at the given {@code resultConsumer}.
   *
   * <p>The result value will be passed as argument to method {@link #doStoreResult(Task, Object)} by
   * the caller. If the consumer is called multiple times, all but the last call will be ignored. If the consumer
   * isn't called, then method {@link #doStoreResult(Task, Object)} won't be called and workflow variables won't be
   * changed as result of this action.
   *
   * <p>If this method throws a {@link GCFacadeException} or {@link GlobalLinkWorkflowException}, then method
   * {@link #doStoreResult(Task, Object)} will still be called afterward if a result has been set at the
   * consumer. In case of other exceptions, no results are stored and exceptions are propagated and
   * may lead to task escalation, depending on the value of the {@code rethrowResultException} constructor
   * parameter.
   *
   * <p>This method is called from {@link com.coremedia.cap.workflow.plugin.LongAction#execute(Object)} and
   * the constraints documented for that method apply here as well.
   *
   * @param params         parameters returned by {@link #doExtractParameters(Task)}
   * @param resultConsumer consumer that takes the result of the execution
   * @param facade         the facade to communicate with GlobalLink
   * @param issues         map to add issues to that occurred during action execution and will be stored in the workflow
   *                       variable set with {@link #setIssuesVariable(String)}. The workflow can display
   *                       these issues to the end-user, who may trigger a retry, for example.
   * @throws GCFacadeException           if an error was raised by the given facade
   * @throws GlobalLinkWorkflowException if some other error occurred
   */
  abstract void doExecuteGlobalLinkAction(P params,
                                          Consumer<? super R> resultConsumer,
                                          GCExchangeFacade facade,
                                          Map<String, List<Content>> issues);

  /**
   * Receives the result from {@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)} if that
   * method passed a result to its consumer argument. This method may store the result in workflow variables.
   * It may also return a value that the caller then stores in the result workflow variable, which is configured in the
   * workflow definition with attribute {@code resultVariable} at the {@code Action} element.
   *
   * <p>This method is called from {@link com.coremedia.cap.workflow.plugin.LongAction#storeResult(Task, Object)} and
   * the constraints documented for that method apply here as well.
   *
   * <p>The default implementation just returns the unchanged value of the {@code result} argument. Subclasses
   * must override this method for complex result values that cannot or should not be stored in the
   * {@code resultVariable} as is.
   *
   * @param task   the task in which the action should be executed
   * @param result result value that was passed to the consumer in {@link #doExecuteGlobalLinkAction}
   * @return value to store in the {@code resultVariable} or null to store nothing in that variable
   */
  @Nullable
  Object doStoreResult(Task task, R result) {
    return result;
  }

  // --- Helper methods for subclasses ----------------------------------------

  SitesService getSitesService() {
    return getSpringContext().getBean(SitesService.class);
  }

  static long parseSubmissionId(String submissionId, String wfTaskId) {
    if (submissionId == null || submissionId.isEmpty()) {
      throw new GlobalLinkWorkflowException(GlobalLinkWorkflowErrorCodes.ILLEGAL_SUBMISSION_ID_ERROR, "GlobalLink submission id not set", wfTaskId);
    }
    try {
      return Long.parseLong(submissionId);
    } catch (NumberFormatException e) {
      throw new GlobalLinkWorkflowException(GlobalLinkWorkflowErrorCodes.ILLEGAL_SUBMISSION_ID_ERROR, "GlobalLink submission id malformed. Long value expected",
        wfTaskId, submissionId);
    }
  }

  static MimeType mimeType(String mimeTypeString) {
    try {
      return new MimeType(mimeTypeString);
    } catch (MimeTypeParseException e) {
      throw new IllegalArgumentException("Cannot parse mime-type: '" + mimeTypeString + "'.", e);
    }
  }

  // --- Internal -------------------------------------------------------------

  private Site getMasterSite(@NonNull Collection<? extends ContentObject> masterContents) {
    SitesService sitesService = getSitesService();
    return masterContents.stream()
      .map(sitesService::getSiteAspect)
      .map(ContentObjectSiteAspect::getSite)
      .filter(Objects::nonNull)
      .findAny()
      .orElseThrow(() -> new IllegalStateException("No master site found"));
  }

  private static int maxAutomaticRetries(@NonNull Settings settings) {
    return settings.at(CONFIG_RETRY_COMMUNICATION_ERRORS)
      .map(v -> {
        try {
          return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
          LOG.warn("Ignoring setting '{}'. Not an integer: {}", CONFIG_RETRY_COMMUNICATION_ERRORS, v);
          return null;
        }
      })
      .orElse(DEFAULT_RETRY_COMMUNICATION_ERRORS);
  }

  @VisibleForTesting
  @NonNull
  Settings withGlobalSettings(@NonNull Settings base,
                              @NonNull ContentRepository repository) {
    return base.putAll(SettingsSource.fromPath(repository, GLOBAL_CONFIGURATION_PATH));
  }

  @VisibleForTesting
  @NonNull
  Settings withSiteSettings(@NonNull Settings base,
                            @NonNull Site site) {
    return base.putAll(SettingsSource.fromPathAtSite(site, SITE_CONFIGURATION_PATH));
  }

  @NonNull
  private GCExchangeFacade openSession(@NonNull Settings settings) {
    return openSession(settings.properties());
  }

  @VisibleForTesting
  @NonNull
  GCExchangeFacade openSession(@NonNull Map<String, Object> settings) {
    return defaultFactory().openSession(settings);
  }

  /**
   * Returns a {@link Result} object to trigger a retry in case of (temporary) CMS connection errors. Re-throws the
   * given exception, if another error.
   *
   * @param settings  settings to consider; typically only considers retry delay
   *                  configuration available from the Spring context
   * @param exception the exception to handle.
   * @param result    the execution result so far.
   */
  private Result<R> getResultForCMSConnectionError(@NonNull Settings settings,
                                                   @NonNull RuntimeException exception,
                                                   @NonNull Result<R> result) {
    // if exception is not indicating a curable CMS connection error situation, re-throw it without configuring a retry
    if (!isRepositoryUnavailableException(exception)) {
      throw exception;
    }
    // get delay for retries on CMS connection error *just* from properties
    int cmsRetryDelaySeconds = getRetryDelay(settings, CMS_RETRY_DELAY_SETTINGS_KEY).toSecondsInt();
    LOG.info("{}: Failed to connect to CMS. Will retry after {} seconds.", getName(), cmsRetryDelaySeconds, exception);
    result.remainingAutomaticRetries = Integer.MAX_VALUE;
    result.retryDelaySeconds = cmsRetryDelaySeconds;
    // issue type is irrelevant, it's just required to have *some* issue
    Map<String, List<Content>> issues = new HashMap<>();
    issues.put(GlobalLinkWorkflowErrorCodes.CMS_COMMUNICATION_ERROR, List.of());
    result.issues = issuesAsJsonBlob(issues);
    return result;
  }

  @VisibleForTesting
  static boolean isRepositoryUnavailableException(Exception exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof RepositoryNotAvailableException
        || cause instanceof SystemException
        || isCapExceptionWithMatchingErrorCode(cause)) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  static boolean isCapExceptionWithMatchingErrorCode(Throwable throwable) {
    return throwable instanceof CapException
      && ((CapException) throwable).getErrorCode() != null
      && REPOSITORY_UNAVAILABLE_ERROR_CODES.contains(((CapException) throwable).getErrorCode());
  }

  /**
   * Returns a {@link Result} object to trigger a retry in case of GCC connection errors.
   *
   * @param exception           the GlobalLink exception to handle.
   * @param result              the execution result so far.
   * @param issues              issues in execution so far.
   * @param parameters          action parameters.
   * @param retryDelay          time to wait before retrying the GlobalLink action.
   * @param maxAutomaticRetries maximum number of retries for the current GlobalLink action.
   */
  private Result<R> getResultForGCCConnectionError(GCFacadeCommunicationException exception, Result<R> result,
                                                   Map<String, List<Content>> issues, Parameters<P> parameters,
                                                   RetryDelay retryDelay, int maxAutomaticRetries) {
    if (issues.isEmpty()) {
      boolean isInRetryLoop =
        parameters.remainingAutomaticRetries > 0 && parameters.remainingAutomaticRetries != Integer.MAX_VALUE;
      result.remainingAutomaticRetries = isInRetryLoop
        ? parameters.remainingAutomaticRetries - 1
        : maxAutomaticRetries;
    }
    if (result.remainingAutomaticRetries > 0) {
      LOG.info("{}: Failed to connect to GCC ({}). Will retry {} time(s) with {} seconds delay.", getName(),
        GlobalLinkWorkflowErrorCodes.GLOBAL_LINK_COMMUNICATION_ERROR, result.remainingAutomaticRetries,
        retryDelay.toSeconds(), exception);
    } else {
      LOG.warn("{}: Failed to connect to GCC ({}).", getName(), GlobalLinkWorkflowErrorCodes.GLOBAL_LINK_COMMUNICATION_ERROR, exception);
    }
    result.retryDelaySeconds = retryDelay.toSecondsInt();
    issues.put(GlobalLinkWorkflowErrorCodes.GLOBAL_LINK_COMMUNICATION_ERROR, List.of());
    result.issues = issuesAsJsonBlob(issues);
    return result;
  }

  @VisibleForTesting
  @Nullable
  Blob issuesAsJsonBlob(Map<String, List<Content>> issues) {
    if (issues.isEmpty()) {
      return null;
    }

    // all issues should have the severity ERROR when displayed in Studio
    Map<Severity, Map<String, List<Content>>> studioIssues = Map.of(Severity.ERROR, issues);

    byte[] bytes = issuesAsJsonString(studioIssues).getBytes(StandardCharsets.UTF_8);
    return getConnection().getBlobService().fromBytes(bytes, MIME_TYPE_JSON);
  }

  @NonNull
  @VisibleForTesting
  static String issuesAsJsonString(Map<Severity, Map<String, List<Content>>> issues) {
    Type typeToken = new TypeToken<Map<Severity, Map<XliffImportResultCode, List<Content>>>>() {
    }.getType();
    return contentObjectReturnsIdGson.toJson(issues, typeToken);
  }

  private static class ContentObjectSerializer implements JsonSerializer<ContentObject> {
    @Override
    public JsonElement serialize(ContentObject contentObject, Type type, JsonSerializationContext jsonSerializationContext) {
      if (contentObject == null) {
        return JsonNull.INSTANCE;
      }
      return new JsonPrimitive(contentObject.getId());
    }
  }

  @VisibleForTesting
  record Parameters<P>(@Nullable P extendedParameters,
                       @NonNull Collection<ContentObject> masterContentObjects,
                       int remainingAutomaticRetries) {
  }

  @VisibleForTesting
  static class Result<R> {
    /**
     * Holds the result from {@link #doExecuteGlobalLinkAction}, empty for no result
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // suppress warning for non-typical usage of Optional
      Optional<R> extendedResult = Optional.empty();
    /**
     * JSON with a map from studio severity to a map of error codes to possibly empty list of affected contents
     */
    Blob issues;
    /**
     * Number of remaining automatic retries, if there are issues.
     * A value of {@link Integer#MAX_VALUE} signals, that the current
     * GlobalLink Connection Retry is or was interrupted by a CMS connection
     * error. Once this is fixed, the remaining retries should be set to their
     * initial value again.
     */
    int remainingAutomaticRetries;
    /**
     * Seconds to delay before next retry
     */
    int retryDelaySeconds;
  }
}
