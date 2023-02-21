package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.Blob;
import com.coremedia.cap.common.RelativeTimeLimit;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.translate.xliff.XliffImportResultCode;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.cap.workflow.plugin.ActionResult;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCFacadeAccessException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeFileTypeConfigException;
import com.coremedia.labs.translation.gcc.facade.GCFacadeIOException;
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
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.coremedia.labs.translation.gcc.facade.DefaultGCExchangeFacadeSessionProvider.defaultFactory;
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

  private static final long serialVersionUID = -7130959823193680910L;

  private static final String CMSETTINGS_SETTINGS = "settings";
  private static final String CM_SETTINGS = "CMSettings";

  /**
   * Defines the global configuration path.
   * The integration will look up a 'GlobalLink' settings document in this folder.
   */
  private static final String GLOBAL_CONFIGURATION_PATH = "/Settings/Options/Settings/Translation Services";

  /**
   * Defines the site specific configuration path.
   * If a GlobalLink parameter should be different in a specific site,
   * then the 'GlobalLink' settings document can additionally be but in this subfolder of the site.
   */
  private static final String SITE_CONFIGURATION_PATH = "/Options/Settings/Translation Services";


  /**
   * Name of the config parameter in {@link #getGccSettings(Site)} to control how many times communication
   * errors should be retried automatically. Defaults to {@link #DEFAULT_RETRY_COMMUNICATION_ERRORS} if unset.
   */
  private static final String CONFIG_RETRY_COMMUNICATION_ERRORS = "retryCommunicationErrors";
  private static final int DEFAULT_RETRY_COMMUNICATION_ERRORS = 5;

  private static final MimeType MIME_TYPE_JSON = mimeType("application/json");
  private static final Gson contentObjectReturnsIdGson = new GsonBuilder()
          .enableComplexMapKeySerialization()
          .registerTypeAdapter(Content.class, new ContentObjectSerializer())
          .create();

  private static final String CMS_RETRY_DELAY_SETTINGS_KEY = "cms-retry-delay";

  private static final String DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY = "gcc-retry-delay";

  /**
   * Minimum delay between retrying communication with GlobalLink. Firing too many update requests on the external system
   * could be considered a DoS attack.
   */
  private static final int MIN_RETRY_DELAY_SECS = 60; // one minute

  /**
   * If the value is accidentally set to a very big delay and the workflow process picks this value, you will have
   * to wait very long until it checks again for an update.
   * Changing this accidentally got also a lot more likely, since times can be change in the content repository directly.
   */
  private static final int MAX_RETRY_DELAY_SECS = 86400; // one day

  /**
   * Fallback delay between retrying communication with GlobalLink for illegal values.
   */
  private static final int FALLBACK_RETRY_COMMUNICATION_DELAY_SECS = 900;

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

  private static Integer ensureRetryDelayConfig(Map<String, Object> config, String key) {
    Object value = config.get(key);
    if (value == null) {
      LOG.warn("\"{}\" value must not be null. Falling back to {}.", key, FALLBACK_RETRY_COMMUNICATION_DELAY_SECS);
      return FALLBACK_RETRY_COMMUNICATION_DELAY_SECS;
    }
    int retryDelayInSec = Integer.parseInt(String.valueOf(value));
    if (retryDelayInSec < MIN_RETRY_DELAY_SECS) {
      LOG.warn("\"{}\" must not be smaller than {} seconds, but is {}. Falling back to minimum.", key, MIN_RETRY_DELAY_SECS, retryDelayInSec);
      retryDelayInSec = MIN_RETRY_DELAY_SECS;
    } else if (retryDelayInSec > MAX_RETRY_DELAY_SECS) {
      LOG.warn("\"{}\" must not be bigger than {} seconds, but is {}. Falling back to maximum.", key, MAX_RETRY_DELAY_SECS, retryDelayInSec);
      retryDelayInSec = MAX_RETRY_DELAY_SECS;
    }
    return retryDelayInSec;
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

    int gccRetryDelaySeconds = FALLBACK_RETRY_COMMUNICATION_DELAY_SECS;
    int maxAutomaticRetries = 0; // if we ever get to this variable's usage, it will be set to something reasonable
    try {
      Site masterSite = getMasterSite(parameters.masterContentObjects);
      Map<String, Object> settings = getGccSettings(masterSite);
      gccRetryDelaySeconds = ensureRetryDelayConfig(settings, getGCCRetryDelaySettingsKey());
      maxAutomaticRetries = maxAutomaticRetries(masterSite);

      GCExchangeFacade gccSession = openSession(masterSite);

      // call subclass implementation and store the result as result.extendedResult
      Consumer<R> resultConsumer = r -> result.extendedResult = Optional.of(r);
      doExecuteGlobalLinkAction(parameters.extendedParameters, resultConsumer, gccSession, issues);

    } catch (GCFacadeCommunicationException e) {
      // automatically retry upon communication errors until configured maximum of retries has been reached
      // but do not retry automatically if #doExecuteGlobalLinkAction returned additional issues
      return getResultForGCCConnectionError(e, issues, parameters, gccRetryDelaySeconds, maxAutomaticRetries);
    } catch (GCFacadeIOException e) {
      LOG.warn("{}: Local I/O error ({}).", getName(), GlobalLinkWorkflowErrorCodes.LOCAL_IO_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.LOCAL_IO_ERROR, Collections.emptyList());
    } catch (GCFacadeFileTypeConfigException e) {
      LOG.warn("{}: Communication failed because of unsupported configured file type ({})", getName(), GlobalLinkWorkflowErrorCodes.SETTINGS_FILE_TYPE_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.SETTINGS_FILE_TYPE_ERROR, Collections.emptyList());
    } catch (GCFacadeConfigException e) {
      LOG.warn("{}: Communication failed because of invalid/missing settings ({})", getName(), GlobalLinkWorkflowErrorCodes.SETTINGS_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.SETTINGS_ERROR, Collections.emptyList());
    } catch (GCFacadeAccessException e) {
      LOG.warn("{}: Authentication with API key failed ({})", getName(), GlobalLinkWorkflowErrorCodes.INVALID_KEY_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.INVALID_KEY_ERROR, Collections.emptyList());
    } catch (GCFacadeException e) {
      LOG.warn("{}: Unknown error occurred ({})", getName(), GlobalLinkWorkflowErrorCodes.UNKNOWN_ERROR, e);
      issues.put(GlobalLinkWorkflowErrorCodes.UNKNOWN_ERROR, Collections.emptyList());
    } catch (GlobalLinkWorkflowException e) {
      LOG.warn("{}: " + e.getMessage() + "({})", getName(), e.getErrorCode(), e);
      issues.put(e.getErrorCode(), Collections.emptyList());
    } catch (RuntimeException e) {
      // automatically retry upon CMS connection errors
      return getResultForCMSConnectionError(e);
    }

    result.retryDelaySeconds = gccRetryDelaySeconds;
    result.issues = issuesAsJsonBlob(issues);
    return result;
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

    LOG.info("action result retries {}, delay {}", r.remainingAutomaticRetries, r.retryDelaySeconds);
    Process process = task.getContainingProcess();
    process.set(remainingAutomaticRetriesVariable, r.remainingAutomaticRetries);
    process.set(retryDelayTimerVariable, new RelativeTimeLimit(r.retryDelaySeconds));
    process.set(issuesVariable, r.issues);

    Object resultValue = r.extendedResult
            .map(extendedResult -> doStoreResult(task, extendedResult))
            .orElse(null);
    return super.storeResult(task, resultValue);
  }

  // --- Methods to be implemented / overridden by concrete subclass -------------------------------

  /**
   * Extract parameters from the {@link Task} and return them as a single object of type {@code <T>}.
   * The result will be passed as argument to method {@link #doExecuteGlobalLinkAction(Object, Consumer, GCExchangeFacade, Map)}.
   *
   * <p>This method is called from {@link com.coremedia.cap.workflow.plugin.LongAction#extractParameters(Task)} and
   * the constraints documented for that method apply here as well.
   *
   * @param task the task in which the action should be executed
   * @return the parameters for the actual computation or null if no parameters are needed
   */
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
   * {@link #doStoreResult(Task, Object)} will still be called afterwards if a result has been set at the
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

  private Site getMasterSite(Collection<? extends ContentObject> masterContents) {
    SitesService sitesService = getSitesService();
    return masterContents.stream()
            .map(sitesService::getSiteAspect)
            .map(ContentObjectSiteAspect::getSite)
            .filter(Objects::nonNull)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No master site found"));
  }

  private int maxAutomaticRetries(Site masterSite) {
    Map<String, Object> gccSettings = getGccSettings(masterSite);
    Object value = gccSettings.get(CONFIG_RETRY_COMMUNICATION_ERRORS);
    if (value != null) {
      try {
        return Integer.parseInt(String.valueOf(value));
      } catch (NumberFormatException e) {
        LOG.warn("Ignoring setting '{}'. Not an integer: {}", CONFIG_RETRY_COMMUNICATION_ERRORS, value);
      }
    }
    return DEFAULT_RETRY_COMMUNICATION_ERRORS;
  }

  @VisibleForTesting
  Map<String, Object> getGccSettings(Site site) {

    Map<String, Object> result = new HashMap<>();

    Map<String, Object> defaultSettings = getGccSettingsFromProperties();
    if (!defaultSettings.isEmpty()) {
      LOG.debug("Applying default settings for GCC connection with keys \"{}\" from properties file.", defaultSettings.keySet());
      result.putAll(defaultSettings);
    }

    // Global configuration
    Map<String, Object> globalSettings = getGccConfigsFromLocation(site, GLOBAL_CONFIGURATION_PATH);
    if (!globalSettings.isEmpty()) {
      LOG.debug("Applying global settings for GCC connection with keys \"{}\".", globalSettings.keySet());
      result.putAll(globalSettings);
    }

    // Site specific configuration overrides global configuration
    Content siteRootFolder = site.getSiteRootFolder();
    String sitePath = siteRootFolder.getPath() + SITE_CONFIGURATION_PATH;
    Map<String, Object> siteSettings = getGccConfigsFromLocation(site, sitePath);
    if (!siteSettings.isEmpty()) {
      LOG.debug("Applying site settings for GCC Connection with keys \"{}\".", globalSettings.keySet());
      result.putAll(siteSettings);
    }

    return Collections.unmodifiableMap(result);
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> getGccSettingsFromProperties() {
    return new HashMap<String, Object>(getSpringContext().getBean("gccConfigurationProperties", Map.class));
  }

  private static Map<String, Object> getGccConfigsFromLocation(Site site, String location) {
    Map<String, Object> result = new HashMap<>();
    for (Content content : getSettingsInTranslationServicesFolder(site, location)) {
      Map<String, Object> struct = getGccConfigFromSetting(content);
      if (struct != null && !struct.isEmpty()) {
        LOG.debug("Found GCC settings \"{}\" in \"{}\".", struct.keySet(), content.getPath());
        result.putAll(struct);
      }
    }
    return result;
  }

  /**
   * Collect contents with configuration settings
   * <p>
   * Returns a CMSettings document itself or all direct CMSettings children
   * of a folder or an empty list otherwise.
   *
   * @return a list of CMSettings contents
   */
  private static Collection<Content> getSettingsInTranslationServicesFolder(Site site, String location) {
    Content root = site.getSiteIndicator().getRepository().getChild(location);
    if (root == null) {
      return Collections.emptyList();
    } else if (root.isFolder()) {
      return root.getChildrenWithType(CM_SETTINGS);
    } else if (root.getType().isSubtypeOf(CM_SETTINGS)) {
      return Collections.singletonList(root);
    } else {
      LOG.info("{} is of type {} and thus no suitable translation service connection configuration content", location, root.getType());
      return Collections.emptyList();
    }
  }

  private static Map<String, Object> getGccConfigFromSetting(Content content) {
    Struct struct = getStruct(content, CMSETTINGS_SETTINGS);
    if (struct != null) {
      Object value = struct.get(GCConfigProperty.KEY_GLOBALLINK_ROOT);
      if (value instanceof Struct) {
        return ((Struct) value).toNestedMaps();
      }
    }
    return Collections.emptyMap();
  }

  @Nullable
  private static Struct getStruct(Content content, String name) {
    if (content != null && content.isInProduction()) {
      return content.getStruct(name);
    }
    return null;
  }

  @VisibleForTesting
  GCExchangeFacade openSession(Site site) {
    return defaultFactory().openSession(getGccSettings(site));
  }

  /**
   * Returns a {@link Result} object to trigger a retry in case of (temporary) CMS connection errors. Re-throws the
   * given exception, if another error.
   */
  private Result<R> getResultForCMSConnectionError(RuntimeException exception) {
    // if exception is not indicating a curable CMS connection error situation, re-throw it without configuring a retry
    if (!RepositoryUnavailableThrowablePredicate.matches(exception)) {
      throw exception;
    }
    LOG.info("{}: Failed to connect to CMS. Will retry.", getName(), exception);
    Result<R> result = new Result<>();
    result.remainingAutomaticRetries = Integer.MAX_VALUE;
    // get delay for retries on CMS connection error *just* from properties
    Map<String, Object> properties = getGccSettingsFromProperties();
    result.retryDelaySeconds = ensureRetryDelayConfig(properties, CMS_RETRY_DELAY_SETTINGS_KEY);
    // issue type is irrelevant, it's just required to have *some* issue
    Map<String, List<Content>> issues = new HashMap<>();
    issues.put(GlobalLinkWorkflowErrorCodes.CMS_COMMUNICATION_ERROR, Collections.emptyList());
    result.issues = issuesAsJsonBlob(issues);
    return result;
  }

  /**
   * Returns a {@link Result} object to trigger a retry in case of GCC connection errors.
   */
  private Result<R> getResultForGCCConnectionError(Exception exception, Map<String, List<Content>> issues,
                                                   Parameters<P> parameters, int gccRetryDelaySeconds, int maxAutomaticRetries) {
    Result<R> result = new Result<>();
    if (issues.isEmpty()) {
      boolean isInRetryLoop =
              parameters.remainingAutomaticRetries > 0 && parameters.remainingAutomaticRetries != Integer.MAX_VALUE;
      result.remainingAutomaticRetries = isInRetryLoop
              ? parameters.remainingAutomaticRetries - 1
              : maxAutomaticRetries;
    }
    if (result.remainingAutomaticRetries > 0) {
      LOG.info("{}: Failed to connect to GCC ({}). Will retry {} time(s).", getName(),
              GlobalLinkWorkflowErrorCodes.GLOBAL_LINK_COMMUNICATION_ERROR, result.remainingAutomaticRetries, exception);
    } else {
      LOG.warn("{}: Failed to connect to GCC ({}).", getName(), GlobalLinkWorkflowErrorCodes.GLOBAL_LINK_COMMUNICATION_ERROR, exception);
    }
    result.retryDelaySeconds = gccRetryDelaySeconds;
    issues.put(GlobalLinkWorkflowErrorCodes.GLOBAL_LINK_COMMUNICATION_ERROR, Collections.emptyList());
    result.issues = issuesAsJsonBlob(issues);
    return result;
  }

  @Nullable
  private Blob issuesAsJsonBlob(Map<String, List<Content>> issues) {
    if (issues.isEmpty()) {
      return null;
    }

    // all issues should have the severity ERROR when displayed in Studio
    Map<Severity, Map<String, List<Content>>> studioIssues = Collections.singletonMap(Severity.ERROR, issues);

    byte[] bytes = issuesAsJsonString(studioIssues).getBytes(StandardCharsets.UTF_8);
    return getConnection().getBlobService().fromBytes(bytes, MIME_TYPE_JSON);
  }

  private static String issuesAsJsonString(Map<Severity, Map<String, List<Content>>> issues) {
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

  private static class Parameters<P> {
    final P extendedParameters;
    final Collection<ContentObject> masterContentObjects;
    final int remainingAutomaticRetries;

    Parameters(P extendedParameters, Collection<ContentObject> masterContentObjects, int remainingAutomaticRetries) {
      this.extendedParameters = extendedParameters;
      this.masterContentObjects = masterContentObjects;
      this.remainingAutomaticRetries = remainingAutomaticRetries;
    }
  }

  private static class Result<R> {
    /** holds the result from {@link #doExecuteGlobalLinkAction}, empty for no result */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // suppress warning for non-typical usage of Optional
    Optional<R> extendedResult = Optional.empty();
    /** json with map from studio severity to map of error codes to possibly empty list of affected contents */
    Blob issues;
    /** number of remaining automatic retries, if there are issues */
    int remainingAutomaticRetries;
    /** seconds to delay before next retry */
    int retryDelaySeconds;
  }
}
