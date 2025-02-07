interface GccWorkflowLocalization_properties {
  TranslationGlobalLink_displayName: string;
  TranslationGlobalLink_description: string;

  TranslationGlobalLink_task_Prepare_displayName: string;
  TranslationGlobalLink_task_AutoMerge_displayName: string;
  TranslationGlobalLink_task_SendTranslationRequest_displayName: string;
  TranslationGlobalLink_task_CancelTranslation_displayName: string;
  TranslationGlobalLink_task_DownloadTranslation_displayName: string;
  TranslationGlobalLink_task_ReviewDeliveredTranslation_displayName: string;
  TranslationGlobalLink_task_ReviewRedeliveredTranslation_displayName: string;
  TranslationGlobalLink_task_ReviewCancelledTranslation_displayName: string;
  TranslationGlobalLink_task_RollbackContent_displayName: string;
  TranslationGlobalLink_task_Complete_displayName: string;
  TranslationGlobalLink_task_Finish_displayName: string;
  TranslationGlobalLink_task_HandleSendTranslationRequestError_displayName: string;
  TranslationGlobalLink_task_HandleDownloadTranslationError_displayName: string;
  TranslationGlobalLink_task_HandleCancelTranslationError_displayName: string;

  TranslationGlobalLink_state_Translate_displayName: string;
  TranslationGlobalLink_state_rollbackTranslation_displayName: string;
  TranslationGlobalLink_state_rollbackTranslation_afterCancellationFailed_displayName: string;
  TranslationGlobalLink_state_finishTranslation_displayName: string;
  TranslationGlobalLink_state_DownloadTranslation_displayName: string;
  TranslationGlobalLink_state_ReviewDeliveredTranslation_displayName: string;
  TranslationGlobalLink_state_ReviewRedeliveredTranslation_displayName: string;
  TranslationGlobalLink_state_ReviewCancelledTranslation_displayName: string;
  TranslationGlobalLink_state_translationReviewed_displayName: string;
  TranslationGlobalLink_state_continueRetry_displayName: string;
  TranslationGlobalLink_state_retryCancellation_displayName: string;
  TranslationGlobalLink_state_continueTranslation_displayName: string;
  TranslationGlobalLink_state_HandleSendTranslationRequestError_displayName: string;
  TranslationGlobalLink_state_HandleDownloadTranslationError_displayName: string;
  TranslationGlobalLink_state_HandleCancelTranslationError_displayName: string;

  "GCC-WF-10000_text": string;
  "GCC-WF-20000_text": string;
  "GCC-WF-30001_text": string;
  "GCC-WF-40000_text": string;
  "GCC-WF-40001_text": string;
  "GCC-WF-40002_text": string;
  "GCC-WF-40003_text": string;
  "GCC-WF-40050_text": string;
  "GCC-WF-50050_text": string;
  "GCC-WF-60000_text": string;
  "GCC-WF-60001_text": string;
  "GCC-WF-61001_text": string;
  dateLiesInPast_globalLinkDueDate_text: string;
  dateInvalid_globalLinkDueDate_text: string;
  SUCCESS_singular_text: string;
  SUCCESS_plural_text: string;
}

const GccWorkflowLocalization_properties: GccWorkflowLocalization_properties = {
  TranslationGlobalLink_displayName: "Translation with GlobalLink",
  TranslationGlobalLink_description: "Translation with GlobalLink",

  TranslationGlobalLink_task_Prepare_displayName: "Preparing",
  TranslationGlobalLink_task_AutoMerge_displayName: "Updating non-translatable fields",
  TranslationGlobalLink_task_SendTranslationRequest_displayName: "Sending translation request",
  TranslationGlobalLink_task_CancelTranslation_displayName: "Awaiting cancelation to finish",
  TranslationGlobalLink_task_DownloadTranslation_displayName: "Awaiting translation results",
  TranslationGlobalLink_task_ReviewDeliveredTranslation_displayName: "Review translation",
  TranslationGlobalLink_task_ReviewRedeliveredTranslation_displayName: "Review translation (redelivered)",
  TranslationGlobalLink_task_ReviewCancelledTranslation_displayName: "Review cancelation",
  TranslationGlobalLink_task_RollbackContent_displayName: "Performing rollback",
  TranslationGlobalLink_task_Complete_displayName: "Updating derived content states",
  TranslationGlobalLink_task_Finish_displayName: "Archiving workflow",
  TranslationGlobalLink_task_HandleSendTranslationRequestError_displayName: "Upload error",
  TranslationGlobalLink_task_HandleDownloadTranslationError_displayName: "Download error",
  TranslationGlobalLink_task_HandleCancelTranslationError_displayName: "Cancelation error",

  TranslationGlobalLink_state_Translate_displayName: "Accept translation workflow",
  TranslationGlobalLink_state_rollbackTranslation_displayName: "Abort and rollback changes",
  TranslationGlobalLink_state_rollbackTranslation_afterCancellationFailed_displayName:
    "Abort and rollback without canceling at GlobalLink.",
  TranslationGlobalLink_state_finishTranslation_displayName: "Finish content localization",
  TranslationGlobalLink_state_DownloadTranslation_displayName: "Awaiting translation results",
  TranslationGlobalLink_state_ReviewDeliveredTranslation_displayName: "Review translation",
  TranslationGlobalLink_state_ReviewRedeliveredTranslation_displayName: "Review translation (redelivered)",
  TranslationGlobalLink_state_ReviewCancelledTranslation_displayName: "Review cancelation",
  TranslationGlobalLink_state_translationReviewed_displayName: "Finish content localization (Translation Reviewed)",
  TranslationGlobalLink_state_continueRetry_displayName: "Continue and retry",
  TranslationGlobalLink_state_retryCancellation_displayName: "Retry cancelation",
  TranslationGlobalLink_state_continueTranslation_displayName: "Continue Translation",
  TranslationGlobalLink_state_HandleSendTranslationRequestError_displayName: "Upload error",
  TranslationGlobalLink_state_HandleDownloadTranslationError_displayName: "Download error",
  TranslationGlobalLink_state_HandleCancelTranslationError_displayName: "Cancelation error",

  "GCC-WF-10000_text": "An unexpected error occurred.",
  "GCC-WF-20000_text": "Error communicating with GlobalLink.",
  "GCC-WF-30001_text": "A local I/O error occurred.",
  "GCC-WF-40000_text": "The GlobalLink configuration settings are inconsistent.",
  "GCC-WF-40001_text": "The GlobalLink configuration settings contain an unsupported value for 'fileType'.",
  "GCC-WF-40002_text": "The connection to GlobalLink failed because of an expired or invalid 'apiKey'.",
  "GCC-WF-40003_text": "The given connector key in the GlobalLink configuration is invalid (property: 'key').",
  "GCC-WF-40050_text": "Encountered an illegal submission ID.",
  "GCC-WF-50050_text": "An XLIFF export failure occurred.",
  "GCC-WF-60000_text": "General submission failure.",
  "GCC-WF-60001_text": "Submission not found. Your GlobalLink configuration may have issues.",
  "GCC-WF-61001_text": "Failure while trying to cancel GlobalLink submission.",
  dateLiesInPast_globalLinkDueDate_text: "Please choose a future Due Date.",
  dateInvalid_globalLinkDueDate_text: "Please enter a valid Due Date.",
  SUCCESS_singular_text: "The translation result has successfully been imported.",
  SUCCESS_plural_text: "The translation results have successfully been imported.",
};

export default GccWorkflowLocalization_properties;
