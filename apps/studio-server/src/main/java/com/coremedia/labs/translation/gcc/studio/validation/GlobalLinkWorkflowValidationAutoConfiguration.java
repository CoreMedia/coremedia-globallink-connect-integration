package com.coremedia.labs.translation.gcc.studio.validation;

import com.coremedia.cap.workflow.TaskState;
import com.coremedia.rest.cap.workflow.validation.WorkflowValidator;
import com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration;
import com.coremedia.rest.cap.workflow.validation.model.ValidationTask;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowStartValidators;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowTaskValidators;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowValidatorsModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration.TASK_ERROR_VALIDATOR;
import static com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration.TRANSLATE_TASK_NAME;
import static com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration.TRANSLATION_START_VALIDATORS;
import static com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration.TRANSLATION_WFNOT_RUNNING;
import static com.coremedia.rest.cap.workflow.validation.configuration.TranslationWorkflowValidationConfiguration.TRANSLATION_WFRUNNING;

@Configuration
@Import(TranslationWorkflowValidationConfiguration.class)
public class GlobalLinkWorkflowValidationAutoConfiguration {

  public static final String GLOBAL_LINK_DUE_DATE_KEY = "globalLinkDueDate";
  public static final String TRANSLATION_GLOBAL_LINK_VALIDATOR_KEY = "TranslationGlobalLink";
  public static final String HANDLE_SEND_TRANSLATION_REQUEST_ERROR = "HandleSendTranslationRequestError";
  public static final String HANDLE_DOWNLOAD_TRANSLATION_ERROR = "HandleDownloadTranslationError";
  public static final String HANDLE_CANCEL_TRANSLATION_ERROR = "HandleCancelTranslationError";

  @Bean
  WorkflowValidatorsModel translationGccWFValidators(@Qualifier(TRANSLATION_START_VALIDATORS) WorkflowStartValidators translationStartValidators,
                                                     @Qualifier(TRANSLATION_WFNOT_RUNNING) List<WorkflowValidator> translationWFNotRunning,
                                                     @Qualifier(TRANSLATION_WFRUNNING) List<WorkflowValidator> translationWFRunning,
                                                     @Qualifier(TASK_ERROR_VALIDATOR) WorkflowValidator taskErrorValidator) {
    ValidationTask runningTask = new ValidationTask(TRANSLATE_TASK_NAME, TaskState.RUNNING);
    ValidationTask waitingTask = new ValidationTask(TRANSLATE_TASK_NAME, TaskState.ACTIVATED);
    ValidationTask sendTranslationRequestErrorTask = new ValidationTask(HANDLE_SEND_TRANSLATION_REQUEST_ERROR);
    ValidationTask downloadTranslationRequestErrorTask = new ValidationTask(HANDLE_DOWNLOAD_TRANSLATION_ERROR);
    ValidationTask cancelTranslationRequestErrorTask = new ValidationTask(HANDLE_CANCEL_TRANSLATION_ERROR);

    WorkflowTaskValidators taskValidators = new WorkflowTaskValidators(
            Map.of(runningTask, translationWFRunning,
                    waitingTask, translationWFNotRunning,
                    sendTranslationRequestErrorTask, List.of(taskErrorValidator),
                    downloadTranslationRequestErrorTask, List.of(taskErrorValidator),
                    cancelTranslationRequestErrorTask, List.of(taskErrorValidator)));


    List<WorkflowValidator> workflowValidators = new ArrayList<>();
    workflowValidators.add(new GCCDateLiesInFutureValidator(GLOBAL_LINK_DUE_DATE_KEY));
    workflowValidators.addAll(translationStartValidators.getWorkflowValidators());
    WorkflowStartValidators gccStartValidators = new WorkflowStartValidators(
            translationStartValidators.getWorkflowValidationPreparation(), workflowValidators
    );

    return new WorkflowValidatorsModel(TRANSLATION_GLOBAL_LINK_VALIDATOR_KEY, taskValidators, gccStartValidators);
  }
}
