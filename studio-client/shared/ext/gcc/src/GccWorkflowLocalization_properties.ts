interface GccWorkflowLocalization_properties {
  TranslationGlobalLink_displayName: string,
  TranslationGlobalLink_description: string,

  TranslationGlobalLink_task_Prepare_displayName: string,
  TranslationGlobalLink_task_AutoMerge_displayName: string,
  TranslationGlobalLink_task_SendTranslationRequest_displayName: string,
  TranslationGlobalLink_task_DownloadTranslation_displayName: string,
  TranslationGlobalLink_task_ReviewDeliveredTranslation_displayName: string,
  TranslationGlobalLink_task_ReviewCancelledTranslation_displayName: string,
  TranslationGlobalLink_task_RollbackContent_displayName: string,
  TranslationGlobalLink_task_Complete_displayName: string,
  TranslationGlobalLink_task_Finish_displayName: string,
  TranslationGlobalLink_task_HandleSendTranslationRequestError_displayName: string,
  TranslationGlobalLink_task_HandleDownloadTranslationError_displayName: string,
  TranslationGlobalLink_task_HandleCancelTranslationError_displayName: string,

  TranslationGlobalLink_state_Translate_displayName: string,
  TranslationGlobalLink_state_rollbackTranslation_displayName: string,
  TranslationGlobalLink_state_rollbackTranslation_afterCancellationFailed_displayName: string,
  TranslationGlobalLink_state_finishTranslation_displayName: string,
  TranslationGlobalLink_state_DownloadTranslation_displayName: string,
  TranslationGlobalLink_state_ReviewDeliveredTranslation_displayName: string,
  TranslationGlobalLink_state_ReviewCancelledTranslation_displayName: string,
  TranslationGlobalLink_state_translationReviewed_displayName: string,
  TranslationGlobalLink_state_continueRetry_displayName: string,
  TranslationGlobalLink_state_HandleSendTranslationRequestError_displayName: string,
  TranslationGlobalLink_state_HandleDownloadTranslationError_displayName: string,
  TranslationGlobalLink_state_HandleCancelTranslationError_displayName: string,

  "GCC-WF-10000_text": string,
  "GCC-WF-20000_text": string,
  "GCC-WF-30001_text": string,
  "GCC-WF-40000_text": string,
  "GCC-WF-40001_text": string,
  "GCC-WF-40050_text": string,
  "GCC-WF-50050_text": string,
  "GCC-WF-61001_text": string,
  dateLiesInPast_globalLinkDueDate_text: string,
  dateInvalid_globalLinkDueDate_text: string,
  SUCCESS_singular_text: string,
  SUCCESS_plural_text: string,
}

const GccWorkflowLocalization_properties: GccWorkflowLocalization_properties = {
  TranslationGlobalLink_displayName: "Translation with GlobalLink",
  TranslationGlobalLink_description: "Translation with GlobalLink",

  TranslationGlobalLink_task_Prepare_displayName: "Preparing",
  TranslationGlobalLink_task_AutoMerge_displayName: "Updating Non-Translatable Fields",
  TranslationGlobalLink_task_SendTranslationRequest_displayName: "Sending Translation Request",
  TranslationGlobalLink_task_DownloadTranslation_displayName: "Awaiting Translation Results",
  TranslationGlobalLink_task_ReviewDeliveredTranslation_displayName: "Review Translation",
  TranslationGlobalLink_task_ReviewCancelledTranslation_displayName: "Review Cancellation",
  TranslationGlobalLink_task_RollbackContent_displayName: "Performing Rollback",
  TranslationGlobalLink_task_Complete_displayName: "Updating Derived Content States",
  TranslationGlobalLink_task_Finish_displayName: "Archiving Workflow",
  TranslationGlobalLink_task_HandleSendTranslationRequestError_displayName: "Upload Error",
  TranslationGlobalLink_task_HandleDownloadTranslationError_displayName: "Download Error",
  TranslationGlobalLink_task_HandleCancelTranslationError_displayName: "Cancellation Error",

  TranslationGlobalLink_state_Translate_displayName: "Accept Translation Workflow",
  TranslationGlobalLink_state_rollbackTranslation_displayName: "Reject Changes",
  TranslationGlobalLink_state_rollbackTranslation_afterCancellationFailed_displayName: "Reject Changes without cancelling the submission at GlobalLink.",
  TranslationGlobalLink_state_finishTranslation_displayName: "Finish Content Localization",
  TranslationGlobalLink_state_DownloadTranslation_displayName: "Awaiting Translation Results",
  TranslationGlobalLink_state_ReviewDeliveredTranslation_displayName: "Review Translation",
  TranslationGlobalLink_state_ReviewCancelledTranslation_displayName: "Review Cancellation",
  TranslationGlobalLink_state_translationReviewed_displayName: "Finish Content Localization (Translation Reviewed)",
  TranslationGlobalLink_state_continueRetry_displayName: "Continue and Retry",
  TranslationGlobalLink_state_HandleSendTranslationRequestError_displayName: "Upload Error",
  TranslationGlobalLink_state_HandleDownloadTranslationError_displayName: "Download Error",
  TranslationGlobalLink_state_HandleCancelTranslationError_displayName: "Cancellation Error",

  "GCC-WF-10000_text": "An unexpected error occurred.",
  "GCC-WF-20000_text": "Error communicating with GlobalLink.",
  "GCC-WF-30001_text": "A local IO error occurred.",
  "GCC-WF-40000_text": "The GlobalLink configuration settings are inconsistent.",
  "GCC-WF-40001_text": "The GlobalLink configuration settings contain an unsupported value for 'fileType'.",
  "GCC-WF-40050_text": "Encountered an illegal submission ID.",
  "GCC-WF-50050_text": "An xliff export failure occurred.",
  "GCC-WF-61001_text": "Failure while trying to cancel GlobalLink submission.",
  dateLiesInPast_globalLinkDueDate_text: "Please choose a future Due Date.",
  dateInvalid_globalLinkDueDate_text: "Please enter a valid Due Date.",
  SUCCESS_singular_text: "The translation result has successfully been imported.",
  SUCCESS_plural_text: "The translation results have successfully been imported.",
};

export default GccWorkflowLocalization_properties;
