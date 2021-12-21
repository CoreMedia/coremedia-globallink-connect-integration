package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCFacadeException;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.labs.translation.gcc.util.Zipper;
import com.coremedia.cap.common.Blob;
import com.coremedia.cap.common.CapSession;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.multisite.SiteModel;
import com.coremedia.cap.translate.xliff.CapXliffImportException;
import com.coremedia.cap.translate.xliff.XliffImportResultCode;
import com.coremedia.cap.translate.xliff.XliffImportResultItem;
import com.coremedia.cap.translate.xliff.XliffImporter;
import com.coremedia.cap.user.User;
import com.coremedia.cap.user.UserRepository;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.CANCELLATION_CONFIRMED;
import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.CANCELLED;
import static com.coremedia.labs.translation.gcc.facade.GCSubmissionState.DELIVERED;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.DUPLICATE_NAME;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.EMPTY_TRANSUNIT_TARGET;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.FAILED;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.INVALID_INTERNAL_LINK;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.INVALID_LOCALE;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.SUCCESS;
import static java.util.Objects.requireNonNull;

/**
 * Workflow action that downloads results from the translation service if the translation job is complete.
 */
public class DownloadFromGlobalLinkAction extends GlobalLinkAction<DownloadFromGlobalLinkAction.Parameters, DownloadFromGlobalLinkAction.Result> {

  private static final long serialVersionUID = 5160741359795894412L;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String WORKING_DIR_PREFIX = "cmsgccwf";
  // Corresponds to Studio's "Upload Files" magic name functionality
  private static final String XLIFF_FILE_EXTENSION = "xliff";
  @VisibleForTesting
  static final String NEWXLIFFS = "newxliffs";
  private static final MimeType MIME_TYPE_ZIP = mimeType("application/zip");

  /**
   * List of {@link XliffImportResultCode}s that are not treated as errors. They won't be displayed to the
   * editor in a workflow user task.
   *
   * <p>Project implementations may want to adapt this list depending on their requirements.
   */
  private static final Set<XliffImportResultCode> IGNORED_XLIFF_IMPORT_RESULT_CODES = EnumSet.of(
          // not an error
          SUCCESS,

          // Duplicate names are resolved automatically by applying a corresponding naming pattern.
          // You may as well decide, that you want to forbid such duplication and remove it from this list.
          DUPLICATE_NAME,

          INVALID_INTERNAL_LINK,
          INVALID_LOCALE,
          EMPTY_TRANSUNIT_TARGET,
          EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE
  );

  private String globalLinkSubmissionIdVariable;
  private String globalLinkPdSubmissionIdsVariable;
  private String globalLinkSubmissionStatusVariable;
  private String xliffResultVariable;
  private String completedLocalesVariable;
  private String cancellationAllowedVariable;

  // --- construct and configure ----------------------------------------------------------------------

  @SuppressWarnings("WeakerAccess") // created from workflow definition
  public DownloadFromGlobalLinkAction() {
    // Escalate in case of errors.
    // Some exceptions that are worth retrying are handled in doExecute.
    super(true);
  }

  /**
   * Sets the name of the Blob process variable in which to store the
   * downloaded xliff file.
   *
   * @param xliffResultVariable variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setXliffResultVariable(String xliffResultVariable) {
    this.xliffResultVariable = xliffResultVariable;
  }

  /**
   * Sets the name of the string process variable holding the internal ID of the translation submission.
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
   * Sets the name of the String process variable that represents the already translated locales
   *
   * @param completedLocalesVariable list workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCompletedLocalesVariable(String completedLocalesVariable) {
    this.completedLocalesVariable = completedLocalesVariable;
  }

  /**
   * Sets the name of the String process variable that states if a workflow may be cancelled
   *
   * @param cancellationAllowedVariable boolean workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCancellationAllowedVariable(String cancellationAllowedVariable) {
    this.cancellationAllowedVariable = cancellationAllowedVariable;
  }


  // --- GlobalLinkAction interface ----------------------------------------------------------------------

  @Override
  Parameters doExtractParameters(Task task) {
    Process process = task.getContainingProcess();

    //Reset xliffResultVariable in order to just provide actual result
    process.set(xliffResultVariable, null);
    String submissionId = process.getString(globalLinkSubmissionIdVariable);

    Set<Locale> completedLocales = process.getStrings(completedLocalesVariable).stream()
            .map(Locale::forLanguageTag)
            .collect(Collectors.toCollection(HashSet::new));

    boolean cancellationAllowed = process.getBoolean(cancellationAllowedVariable);

    return new Parameters(parseSubmissionId(submissionId, task.getId()), completedLocales, cancellationAllowed);
  }

  @Override
  void doExecuteGlobalLinkAction(Parameters params,
                                 Consumer<? super Result> resultConsumer,
                                 GCExchangeFacade facade, Map<String, List<Content>> issues) {
    long submissionId = params.submissionId;

    // We need to share xliff files between #doExecuteGlobalLinkAction and #doStoreResult.
    // Since xliff files may be pretty big, we do not store them in the result
    // directly, but write them into this temp directory and put the
    // temp directory in the result.
    Result result = new Result(prepareWorkingDir());
    result.completedLocales = params.completedLocales;
    result.cancellationAllowed = params.cancellationAllowed;

    try {
      resultConsumer.accept(result);
      doExecuteGlobalLinkAction(facade, submissionId, result, issues);
    } catch (GCFacadeException | GlobalLinkWorkflowException e) {
      throw e; // do not delete working directory, it's needed and deleted in #doStoreResult
    } catch (RuntimeException e) {
      forceDelete(result.workingDir); // #doStoreResult is not called for arbitrary exceptions, must clean up here
      throw e;
    }
  }

  private void doExecuteGlobalLinkAction(GCExchangeFacade facade, long submissionId, Result result,
                                         Map<String, List<Content>> issues) {

    //in case we came from 'HandleDownloadTranslationError' we need to correctly set the "cancellationAllowed" variable first.
    disableCancelWhenCompletedLocalesExist(result);

    GCSubmissionModel submission = facade.getSubmission(submissionId);
    result.globalLinkStatus = submission.getState();
    result.pdSubmissionIds = submission.getPdSubmissionIds();

    if (submission.getState() == CANCELLED) {
      facade.confirmCancelledTasks(submissionId);
      LOG.info("Confirmed cancellation of submission {} (PD ID {}) initiated by GlobalLink.", submissionId, submission.getPdSubmissionIds());
    } else {
      facade.downloadCompletedTasks(submissionId,
              (inputStream, task) -> importXliffFile(inputStream, task, result.completedLocales, issues, result));
      LOG.info("Checked for update of submission {} (PD ID {}) in state {} with completed locales [{}].", submissionId, submission.getPdSubmissionIds(), submission.getState(), result.completedLocales);
      //disable cancel if any tasks are completed
      disableCancelWhenCompletedLocalesExist(result);
    }

    // retrieve potentially updated submission after confirming cancellation or download completed task
    submission = facade.getSubmission(submissionId);
    if (submission.getState() != DELIVERED && submission.getState() != CANCELLATION_CONFIRMED) {
      LOG.debug("Submission {} (PD ID {}) in state {} and thus not completed yet.", submissionId, submission.getPdSubmissionIds(), submission.getState());
    }

    result.globalLinkStatus = submission.getState();
  }

  @Override
  Void doStoreResult(Task task, Result result) {
    try {
      Process process = task.getContainingProcess();
      if (result.globalLinkStatus != null) {
        process.set(globalLinkSubmissionStatusVariable, result.globalLinkStatus.toString());
      }

      process.set(globalLinkPdSubmissionIdsVariable, result.pdSubmissionIds);

      process.set(xliffResultVariable, updateXliffsZip(result));

      List<String> completedLocalesStringList = result.completedLocales.stream()
              .map(Locale::toLanguageTag)
              .collect(Collectors.toList());
      process.set(completedLocalesVariable, completedLocalesStringList);

      process.set(cancellationAllowedVariable, result.cancellationAllowed);

      return null;
    } finally {
      forceDelete(result.workingDir);
    }
  }

  // --- Internal ----------------------------------------------------------------------

  private Blob updateXliffsZip(Result result) {
    try {
      File newXliffsZipFile = zipXliffs(result);
      return newXliffsZipFile == null ? null : getConnection().getBlobService().fromFile(newXliffsZipFile, MIME_TYPE_ZIP);
    } catch (Exception e) {
      // The xliffs zip is not essential, but only a goodie for manual analysis.
      // Do not fail upon this exception, the workflow is still valuable.
      LOG.error("Cannot update xliff blob", e);
      return null;
    }
  }

  @VisibleForTesting
  static File zipXliffs(Result result) throws IOException {

    File xliffResultDir = new File(result.workingDir, "result" + System.currentTimeMillis());

    String issueDetailsFileName = "xliff_issue_details";
    moveFiles(result.workingDir, NEWXLIFFS, xliffResultDir, issueDetailsFileName);

    addIssueDetails(xliffResultDir, result.resultItems, issueDetailsFileName);

    File newXliffsZipFile = new File(result.workingDir, "newxliffs-" + System.currentTimeMillis() + ".zip");
    return zipIfNotEmpty(newXliffsZipFile, xliffResultDir);
  }

  private static void addIssueDetails(File xliffResultDir, Map<Long, List<XliffImportResultItem>> resultItems, String issueDetailsFileName) throws IOException {
    if (resultItems != null && resultItems.size() > 0) {
      for (Map.Entry<Long, List<XliffImportResultItem>> longListEntry : resultItems.entrySet()) {
        File itemsDir = new File(xliffResultDir, issueDetailsFileName);
        File itemsFile = new File(itemsDir, longListEntry.getKey() + "-issuedetails.txt");

        ensureDir(itemsDir);
        try (PrintWriter pw = new PrintWriter(itemsFile)) {
          for (XliffImportResultItem item : longListEntry.getValue()) {
            pw.println(item.toString());
          }
        }
      }
    }
  }

  private static File zipIfNotEmpty(File zipFile, File srcDir) {
    String[] files = srcDir.list();
    if (files == null || files.length == 0) {
      return null;
    } else {
      Zipper.zip(zipFile.getAbsolutePath(), srcDir, null);
      return zipFile;
    }
  }

  private static void moveFiles(File srcDir, String srcSubDirName, File targetDir, String targetSubDirName) throws IOException {
    if (srcSubDirName != null) {
      srcDir = new File(srcDir, srcSubDirName);
    }
    if (targetSubDirName != null) {
      targetDir = new File(targetDir, targetSubDirName);
    }
    File[] files = srcDir.listFiles();
    if (files != null && files.length > 0) {
      ensureDir(targetDir);
      for (File file : files) {
        Files.move(file.toPath(), new File(targetDir, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  private static void ensureDir(File targetDir) throws IOException {
    if (!targetDir.exists() && !targetDir.mkdirs()) {
      throw new IOException("Cannot create directory " + targetDir.getAbsolutePath());
    }
  }

  /**
   * Handler for importing XLIFF from given input stream.
   *
   * @param inputStream                input stream to import from
   * @param xliffImportIssueToContents map to add issues from XLIFF import, which maps error codes to affected contents
   * @param completedLocales           set to add the completed locale from the task
   * @return {@code true} iff. import was completely successful and subsequent actions can continue;
   * {@code false} on any detected problem which requires escalation
   */
  private boolean importXliffFile(InputStream inputStream,
                                  GCTaskModel task,
                                  Set<Locale> completedLocales,
                                  Map<String, List<Content>> xliffImportIssueToContents,
                                  Result result) {

    completedLocales.add(task.getTaskLocale());
    // Mind some data which we possibly need later in #storeResult
    // Save the xliff in a tmp file
    File xliffFile = new File(new File(result.workingDir, NEWXLIFFS), task.getTaskId() + "." + XLIFF_FILE_EXTENSION);
    writeXliffTmpFile(inputStream, xliffFile);

    List<XliffImportResultItem> resultItems;
    XliffImporter importer = getSpringContext().getBean(XliffImporter.class);
    try (InputStream xliffStream = new FileInputStream(xliffFile)) {
      resultItems = asRobotUser(() -> importer.importXliff(xliffStream));
    } catch (CapXliffImportException e) {
      LOG.warn("Failed to import XLIFF", e);
      xliffImportIssueToContents.put(FAILED.toString(), Collections.emptyList());
      return false;
    } catch (IOException e) {
      // Kind of "Cannot happen".
      throw new IllegalStateException("Cannot read temp file " + xliffFile.getAbsolutePath() + ", which we have written just before!", e);
    }

    List<XliffImportResultItem> errorResultItems = resultItems.stream()
            .filter(item -> !IGNORED_XLIFF_IMPORT_RESULT_CODES.contains(item.getCode()))
            .collect(Collectors.toList());

    if (errorResultItems.isEmpty()) {
      // Nothing to record, everything fine.
      forceDelete(xliffFile);
      return true;
    }

    for (XliffImportResultItem errorResultItem : errorResultItems) {
      xliffImportIssueToContents.computeIfAbsent(errorResultItem.getCode().toString(), k -> new ArrayList<>())
              .add(errorResultItem.getContent());
    }

    //store each errorList under its taskID so it can be referenced correctly later
    result.resultItems.put(task.getTaskId(), errorResultItems);
    return false;
  }

  <T> T asRobotUser(Supplier<T> run) {
    User robotUser = getRobotUser();

    // Perform content operations in the name of the robot user.
    final CapSession session = getCapSessionPool().acquireSession(robotUser);
    try {
      final CapSession oldSession = session.activate();
      try {
        return run.get();
      } finally {
        oldSession.activate();
      }
    } finally {
      getCapSessionPool().releaseSession(session);
    }
  }

  /**
   * Returns the user defined in {@link com.coremedia.cap.multisite.SiteModel#getTranslationWorkflowRobotUser()}.
   *
   * @return the translationWorkflowRobotUser
   * @throws java.lang.NullPointerException if the user does not exist.
   */
  private User getRobotUser() {
    final SiteModel siteModel = getSpringContext().getBean(SiteModel.class);
    String robotUserName = siteModel.getTranslationWorkflowRobotUser();
    UserRepository userRepository = requireNonNull(getConnection().getUserRepository(), "UserRepository not available");
    User robotUser = userRepository.getUserByName(robotUserName);
    requireNonNull(robotUser, "No translationWorkflowRobotUser found with name '" + robotUserName + '\'');
    return robotUser;
  }

  private static File prepareWorkingDir() {
    try {
      File workingDir = Files.createTempDirectory(WORKING_DIR_PREFIX).toFile();
      if (!(new File(workingDir, NEWXLIFFS).mkdir())) {
        throw new IllegalStateException("Cannot create subdirectories in temp dir");
      }
      return workingDir;
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create temp dir", e);
    }
  }

  private static void writeXliffTmpFile(InputStream inputStream, File xliffResultFile) {
    try {
      Files.copy(inputStream, xliffResultFile.toPath());
    } catch (IOException e) {
      forceDelete(xliffResultFile);
      throw new IllegalArgumentException("Cannot copy xliff stream to file " + xliffResultFile.getAbsolutePath(), e);
    }
  }

  private static void forceDelete(File file) {
    try {
      FileUtils.forceDelete(file);
    } catch (IOException e) {
      LOG.warn("Cannot delete " + file.getAbsolutePath() + ", please cleanup manually!", e);
    }
  }

  private void disableCancelWhenCompletedLocalesExist(Result result) {
    if (!result.completedLocales.isEmpty()) {
      result.cancellationAllowed = false;
    }
  }

  @VisibleForTesting
  static final class Parameters {
    private long submissionId;
    private Set<Locale> completedLocales;
    private boolean cancellationAllowed;

    Parameters(long submissionId, Set<Locale> completedLocales, boolean cancellationAllowed) {
      this.submissionId = submissionId;
      this.completedLocales = completedLocales;
      this.cancellationAllowed = cancellationAllowed;
    }

  }

  @VisibleForTesting
  static final class Result {
    final File workingDir;


    // Set during xliff import callback
    final Map<Long, List<XliffImportResultItem>> resultItems = new HashMap<>();

    private GCSubmissionState globalLinkStatus;
    private List<String> pdSubmissionIds;
    private Set<Locale> completedLocales;
    private boolean cancellationAllowed;

    Result(File workingDir) {
      this.workingDir = workingDir;
    }
  }
}
