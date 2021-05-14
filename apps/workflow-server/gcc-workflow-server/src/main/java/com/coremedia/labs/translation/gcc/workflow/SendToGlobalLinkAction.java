package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.translate.xliff.XliffExporter;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.translate.item.ContentToTranslateItemTransformer;
import com.coremedia.translate.item.TranslateItem;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.coremedia.labs.translation.gcc.workflow.GlobalLinkWorkflowErrorCodes.XLIFF_EXPORT_FAILURE;
import static com.coremedia.cap.translate.xliff.XliffExportOptions.EmptyOption.EMPTY_IGNORE;
import static com.coremedia.cap.translate.xliff.XliffExportOptions.TargetOption.TARGET_SOURCE;
import static com.coremedia.cap.translate.xliff.XliffExportOptions.xliffExportOptions;
import static com.coremedia.translate.item.TransformStrategy.ITEM_PER_TARGET;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Requests a translation from GlobalLink by opening a submission with uploaded XLIFF for translatable
 * properties of content specified by the workflow.
 */
public class SendToGlobalLinkAction extends GlobalLinkAction<SendToGlobalLinkAction.Parameters, String> {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private static final long serialVersionUID = 7530762957907324426L;

  private String derivedContentsVariable;
  private String subjectVariable;
  private String commentVariable;
  private String globalLinkDueDateVariable;
  private String workflowVariable;
  private boolean mapToBestSupportedLocale;

  // --- construct and configure ----------------------------------------------------------------------

  public SendToGlobalLinkAction() {
    // Escalate in case of errors.
    // Some exceptions that are worth retrying are handled in storeResult
    // and suppressed from escalation.
    super(true);
  }

  /**
   * Sets the name of the process variable that stores the list of derived contents
   * for which a translation should be requested.
   *
   * @param derivedContentsVariable the name of the process variable
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setDerivedContentsVariable(String derivedContentsVariable) {
    this.derivedContentsVariable = derivedContentsVariable;
  }

  /**
   * Sets the variable to read the subject/title of the translation workflow from.
   * This will be used to name the translation submission accordingly.
   *
   * @param subjectVariable subject variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setSubjectVariable(String subjectVariable) {
    this.subjectVariable = subjectVariable;
  }

  /**
   * Sets the variable to read the comment/instructions of the translation workflow from.
   * This will be used for the translation submission accordingly.
   *
   * @param commentVariable subject variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCommentVariable(String commentVariable) {
    this.commentVariable = commentVariable;
  }

  /**
   * Sets the variable to read the dueDate, that will be sent to GlobalLink.
   *
   * @param globalLinkDueDateVariable subject variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setGlobalLinkDueDateVariable(String globalLinkDueDateVariable) {
    this.globalLinkDueDateVariable = globalLinkDueDateVariable;
  }

  /**
   * Sets the variable to define, which translation workflow is used on GlobalLink side.
   *
   * @param workflowVariable workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setWorkflowVariable(String workflowVariable) {
    this.workflowVariable = workflowVariable;
  }

  /**
   * Sets if source and target locales should be mapped to the best matching supported locale.
   *
   * @param mapToBestSupportedLocale if locales should be mapped to the best matching supported locale
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setMapToBestSupportedLocale(boolean mapToBestSupportedLocale) {
    this.mapToBestSupportedLocale = mapToBestSupportedLocale;
  }

  // --- GlobalLinkAction interface ----------------------------------------------------------------------

  @Override
  Parameters doExtractParameters(Task task) {
    Process process = task.getContainingProcess();

    String subject = process.getString(subjectVariable);
    String comment = commentVariable != null ? process.getString(commentVariable) : null;
    List<Content> derivedContents = process.getLinks(derivedContentsVariable);
    List<ContentObject> masterContentObjects = process.getLinksAndVersions(getMasterContentObjectsVariable());
    Calendar date = process.getDate(globalLinkDueDateVariable);
    ZonedDateTime dueDate = ZonedDateTime.ofInstant(date.toInstant(), date.getTimeZone().toZoneId());
    String workflow = workflowVariable != null ? process.getString(workflowVariable) : null;
    String submitter = task.getContainingProcess().getOwner().getName();
    return new Parameters(subject, comment, derivedContents, masterContentObjects, dueDate, workflow, submitter);
  }

  /**
   * Sends a submission to GlobalLink with to be translated XLIFF from the contents specified in the workflow
   * and sets the submission ID as string value as result, which will be stored in the action's {@code resultVariable}.
   *
   * @param params parameters returned by {@link #doExtractParameters(Task)}
   * @param resultConsumer consumer that takes the submission ID as result
   * @param facade the facade to communicate with GlobalLink
   * @param issues map to add issues to that occurred during action execution and will be stored in the workflow
   *               variable set with {@link #setIssuesVariable(String)}. The workflow can display
   *               these issues to the end-user, who may trigger a retry, for example.
   */
  @Override
  void doExecuteGlobalLinkAction(Parameters params, Consumer<? super String> resultConsumer,
                                   GCExchangeFacade facade, Map<String, List<Content>> issues) {
    Collection<Content> derivedContents = params.derivedContents;
    Collection<ContentObject> masterContentObjects = params.masterContentObjects;
    if (derivedContents.isEmpty() || masterContentObjects.isEmpty()) {
      LOG.error("Master and/or derived contents not set. Nothing to translate");
      return;
    }

    Function<ContentObjectSiteAspect, Locale> localeMapper = SendToGlobalLinkAction::preferSiteLocale;
    Map<Locale, List<TranslateItem>> translationItemsByLocale
            = getTranslationItemsByLocale(masterContentObjects, derivedContents, localeMapper);
    Locale firstMasterLocale = findFirstMasterLocale(masterContentObjects, localeMapper)
            .orElseThrow(() -> new IllegalStateException("Unable to identify master locale."));

    String submissionId = submitSubmission(facade, params.subject, params.comment, firstMasterLocale, translationItemsByLocale, params.dueDate, params.workflow, params.submitter);
    resultConsumer.accept(submissionId);
  }

  // --- Internal ----------------------------------------------------------------------

  @SuppressWarnings("NestedTryStatement")
  private Path exportToXliff(Locale sourceLocale, Map.Entry<Locale, List<TranslateItem>> entry) {
    XliffExporter xliffExporter = getSpringContext().getBean(XliffExporter.class);
    String targetLanguageTag = entry.getKey().toLanguageTag();
    List<TranslateItem> items = entry.getValue();

    Path xliffPath;
    try {
      xliffPath = Files.createTempFile(lookup().lookupClass().getSimpleName(),
              '.' + sourceLocale.toLanguageTag() + '2' + targetLanguageTag + ".xliff").toAbsolutePath();

      try (Writer xliffWriter = Files.newBufferedWriter(xliffPath, StandardCharsets.UTF_8)) {
        xliffExporter.exportXliff(
                items,
                xliffWriter,
                xliffExportOptions()
                        .option(EMPTY_IGNORE)
                        .option(TARGET_SOURCE)
                        .build());
      }
      LOG.debug("Exported XLIFF to: '{}'", xliffPath);
    } catch (IOException e) {
      throw new GlobalLinkWorkflowException(XLIFF_EXPORT_FAILURE, "Failed to export XLIFF", e, targetLanguageTag);
    }

    return xliffPath;
  }

  private Optional<Locale> findFirstMasterLocale(Collection<ContentObject> masterObjects, Function<ContentObjectSiteAspect, Locale> localeMapper) {
    SitesService sitesService = getSitesService();
    return masterObjects.stream().map(sitesService::getSiteAspect).map(localeMapper).findFirst();
  }

  private Map<Locale, List<TranslateItem>> getTranslationItemsByLocale(Collection<ContentObject> masterContentObjects,
                                                                       Collection<Content> derivedContents,
                                                                       Function<ContentObjectSiteAspect, Locale> localeMapper) {
    ContentToTranslateItemTransformer transformer = getSpringContext().getBean(ContentToTranslateItemTransformer.class);
    return transformer
            .transform(
                    masterContentObjects,
                    derivedContents,
                    localeMapper,
                    ITEM_PER_TARGET
            )
            .collect(groupingBy(TranslateItem::getSingleTargetLocale));
  }

  /**
   * Create a submission for the given translation items.
   *
   * @param facade                   the facade to communicate with GlobalLink
   * @param subject                  subject, will be part of the submission name
   * @param comment                  comment, will be the instructions of the submission
   * @param sourceLocale             locale of master site
   * @param translationItemsByLocale translation items grouped by target locale
   * @param dueDate                  date that will be sent as 'dueDate' parameter
   * @param workflow                 workflow to be used for the translation, if not the default
   * @param submitter                username of the submitter
   * @return the result that contains the ID of the created submission or an error result
   */
  private String submitSubmission(GCExchangeFacade facade, String subject, String comment,
                                  Locale sourceLocale,
                                  Map<Locale, List<TranslateItem>> translationItemsByLocale,
                                  ZonedDateTime dueDate, String workflow, String submitter) {

    if (mapToBestSupportedLocale) {
      List<Locale> supportedLocales = facade.getSupportedLocales();

      sourceLocale = findBestMatchingLocale(sourceLocale, supportedLocales);

      translationItemsByLocale = translationItemsByLocale.entrySet().stream().collect(
              toMap(entry -> findBestMatchingLocale(entry.getKey(), supportedLocales), entry -> entry.getValue()));
    }

    Map<String, List<Locale>> xliffFileIds = uploadContents(facade, sourceLocale, translationItemsByLocale);

    long submissionId = facade.submitSubmission(subject, comment, dueDate, workflow, submitter, sourceLocale, xliffFileIds);

    LOG.debug("Submitted submission {} for {} files to GCC.", submissionId, xliffFileIds.size());
    return String.valueOf(submissionId);
  }

  private Locale findBestMatchingLocale(Locale locale, List<Locale> supportedLocales) {
    Locale best = locale;
    int bestScore = 0;
    for (Locale supportedLocale : supportedLocales) {
      int score = (locale.getLanguage().equals(supportedLocale.getLanguage()) ? 2 : 0) + (locale.getCountry().equals(supportedLocale.getCountry()) ? 1 : 0);
      if (score > bestScore) {
        best = supportedLocale;
        bestScore = score;
      }
    }
    return best;
  }

  private Map<String, List<Locale>> uploadContents(GCExchangeFacade gccSession, Locale sourceLocale, Map<Locale, List<TranslateItem>> translationItemsByLocale) {
    ImmutableMap.Builder<String, List<Locale>> builder = ImmutableMap.builder();
    for (Map.Entry<Locale, List<TranslateItem>> entry : translationItemsByLocale.entrySet()) {
      String targetLocale = entry.getKey().toLanguageTag();
      Path xliffPath = exportToXliff(sourceLocale, entry);
      String fileName = sourceLocale.toLanguageTag() + '2' + targetLocale + ".xliff";
      try {
        String fileId = gccSession.uploadContent(fileName, new FileSystemResource(xliffPath));
        builder.put(fileId, Collections.singletonList(entry.getKey()));
        LOG.debug(
                "submitSubmission/Upload: Succeeded for {} translation items, target locale {}. Uploaded as fileId {} to GCC.",
                entry.getValue().size(),
                targetLocale,
                fileId);
      } finally {
        tryDeleteIfExists(xliffPath);
      }
    }
    return builder.build();
  }

  private static void tryDeleteIfExists(Path xliffPath) {
    try {
      Files.deleteIfExists(xliffPath);
    } catch (IOException e) {
      LOG.error("Failed to delete temporary XLIFF file: '{}'", xliffPath, e);
    }
  }

  private static Locale preferSiteLocale(ContentObjectSiteAspect aspect) {
    Site site = aspect.getSite();
    if (site == null) {
      return aspect.getLocale();
    }
    return site.getLocale();
  }

  static final class Parameters {
    final String subject;
    final String comment;
    final Collection<Content> derivedContents;
    final Collection<ContentObject> masterContentObjects;
    final ZonedDateTime dueDate;
    final String workflow;
    final String submitter;

    Parameters(String subject,
               String comment,
               Collection<Content> derivedContents,
               Collection<ContentObject> masterContentObjects,
               ZonedDateTime dueDate,
               String workflow,
               String submitter) {
      this.subject = subject;
      this.comment = comment;
      this.derivedContents = derivedContents;
      this.masterContentObjects = masterContentObjects;
      this.dueDate = dueDate;
      this.workflow = workflow;
      this.submitter = submitter;
    }
  }

}
