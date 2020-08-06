package com.coremedia.labs.translation.gcc.studio.validation;

import com.coremedia.cap.workflow.TaskState;
import com.coremedia.rest.cap.workflow.validation.WorkflowValidationConfiguration;
import com.coremedia.rest.cap.workflow.validation.WorkflowValidator;
import com.coremedia.rest.cap.workflow.validation.model.ValidationTask;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowValidatorsModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.coremedia.rest.cap.workflow.validation.WorkflowValidationConfiguration.TRANSLATE_WF_NAME;

@Configuration
@Import(WorkflowValidationConfiguration.class)
public class GlobalLinkWorkflowValidationConfiguration {

  public static final String GLOBAL_LINK_DUE_DATE_KEY = "globalLinkDueDate";
  public static final String TRANSLATION_GLOBAL_LINK_VALIDATOR_KEY = "TranslationGlobalLink";
  public static final String HANDLE_SEND_TRANSLATION_REQUEST_ERROR = "HandleSendTranslationRequestError";
  public static final String HANDLE_DOWNLOAD_TRANSLATION_ERROR = "HandleDownloadTranslationError";
  public static final String HANDLE_CANCEL_TRANSLATION_ERROR = "HandleCancelTranslationError";

  @Bean
  WorkflowValidatorsModel translationGccWFValidators(@Qualifier("translationStartValidators") List<WorkflowValidator> translationStartValidators,
                                                     @Qualifier("translationWFNotRunning") List<WorkflowValidator> translationWFNotRunning,
                                                     @Qualifier("translationWFRunning") List<WorkflowValidator> translationWFRunning,
                                                     @Qualifier("taskErrorValidator") WorkflowValidator taskErrorValidator) {
    ValidationTask runningTask = new ValidationTask(TRANSLATE_WF_NAME, TaskState.RUNNING);
    ValidationTask waitingTask = new ValidationTask(TRANSLATE_WF_NAME, TaskState.ACTIVATED);
    ValidationTask sendTranslationRequestErrorTask = new ValidationTask(HANDLE_SEND_TRANSLATION_REQUEST_ERROR);
    ValidationTask downloadTranslationRequestErrorTask = new ValidationTask(HANDLE_DOWNLOAD_TRANSLATION_ERROR);
    ValidationTask cancelTranslationRequestErrorTask = new ValidationTask(HANDLE_CANCEL_TRANSLATION_ERROR);

    Map<ValidationTask, List<WorkflowValidator>> validators = new HashMap<>();
    validators.put(runningTask, translationWFRunning);
    validators.put(waitingTask, translationWFNotRunning);
    validators.put(sendTranslationRequestErrorTask, List.of(taskErrorValidator));
    validators.put(downloadTranslationRequestErrorTask, List.of(taskErrorValidator));
    validators.put(cancelTranslationRequestErrorTask, List.of(taskErrorValidator));

    List<WorkflowValidator> gccStartValidators = new ArrayList<>(translationStartValidators);
    gccStartValidators.add(new GCCDateLiesInFutureValidator(GLOBAL_LINK_DUE_DATE_KEY));

    return new WorkflowValidatorsModel(TRANSLATION_GLOBAL_LINK_VALIDATOR_KEY, validators, gccStartValidators);
  }
}
