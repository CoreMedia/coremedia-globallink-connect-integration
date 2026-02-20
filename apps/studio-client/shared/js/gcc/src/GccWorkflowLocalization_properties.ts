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
  XLIFF_IMPORT_RESULT_CHECKED_OUT_BY_OTHER_USER_plural_text: string;
  XLIFF_IMPORT_RESULT_CHECKED_OUT_BY_OTHER_USER_singular_text: string;
  XLIFF_IMPORT_RESULT_DOES_NOT_EXIST_plural_text: string;
  XLIFF_IMPORT_RESULT_DOES_NOT_EXIST_singular_text: string;
  XLIFF_IMPORT_RESULT_DUPLICATE_NAME_plural_text: string;
  XLIFF_IMPORT_RESULT_DUPLICATE_NAME_singular_text: string;
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE_plural_text: string;
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE_singular_text: string;
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_plural_text: string;
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_singular_text: string;
  XLIFF_IMPORT_RESULT_FAILED_plural_text: string;
  XLIFF_IMPORT_RESULT_FAILED_singular_text: string;
  XLIFF_IMPORT_RESULT_INVALID_CONTENT_ID_plural_text: string;
  XLIFF_IMPORT_RESULT_INVALID_CONTENT_ID_singular_text: string;
  XLIFF_IMPORT_RESULT_INVALID_INTERNAL_LINK_plural_text: string;
  XLIFF_IMPORT_RESULT_INVALID_INTERNAL_LINK_singular_text: string;
  XLIFF_IMPORT_RESULT_INVALID_LOCALE_plural_text: string;
  XLIFF_IMPORT_RESULT_INVALID_LOCALE_singular_text: string;
  XLIFF_IMPORT_RESULT_INVALID_MARKUP_plural_text: string;
  XLIFF_IMPORT_RESULT_INVALID_MARKUP_singular_text: string;
  XLIFF_IMPORT_RESULT_INVALID_PROPERTY_TYPE_plural_text: string;
  XLIFF_IMPORT_RESULT_INVALID_PROPERTY_TYPE_singular_text: string;
  XLIFF_IMPORT_RESULT_INVALID_XLIFF_plural_text: string;
  XLIFF_IMPORT_RESULT_INVALID_XLIFF_singular_text: string;
  XLIFF_IMPORT_RESULT_LIST_TOO_LONG_plural_text: string;
  XLIFF_IMPORT_RESULT_LIST_TOO_LONG_singular_text: string;
  XLIFF_IMPORT_RESULT_MASTER_CHANGED_plural_text: string;
  XLIFF_IMPORT_RESULT_MASTER_CHANGED_singular_text: string;
  XLIFF_IMPORT_RESULT_MASTER_VERSION_OUTDATED_plural_text: string;
  XLIFF_IMPORT_RESULT_MASTER_VERSION_OUTDATED_singular_text: string;
  XLIFF_IMPORT_RESULT_NOT_AUTHORIZED_plural_text: string;
  XLIFF_IMPORT_RESULT_NOT_AUTHORIZED_singular_text: string;
  XLIFF_IMPORT_RESULT_NO_SUCH_PROPERTY_plural_text: string;
  XLIFF_IMPORT_RESULT_NO_SUCH_PROPERTY_singular_text: string;
  XLIFF_IMPORT_RESULT_STRING_LIST_TOO_LONG_plural_text: string;
  XLIFF_IMPORT_RESULT_STRING_LIST_TOO_LONG_singular_text: string;
  XLIFF_IMPORT_RESULT_STRING_TOO_LONG_plural_text: string;
  XLIFF_IMPORT_RESULT_STRING_TOO_LONG_singular_text: string;
  XLIFF_IMPORT_RESULT_SUCCESS_plural_text: string;
  XLIFF_IMPORT_RESULT_SUCCESS_singular_text: string;
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
  XLIFF_IMPORT_RESULT_CHECKED_OUT_BY_OTHER_USER_plural_text:
    "Some content items could not be imported because they are checked-out by other users.",
  XLIFF_IMPORT_RESULT_CHECKED_OUT_BY_OTHER_USER_singular_text:
    "A content item could not be imported because it is checked-out by another user.",
  XLIFF_IMPORT_RESULT_DOES_NOT_EXIST_plural_text: "Some content items do not exist.",
  XLIFF_IMPORT_RESULT_DOES_NOT_EXIST_singular_text: "A content item does not exist.",
  XLIFF_IMPORT_RESULT_DUPLICATE_NAME_plural_text:
    "Some content items have been renamed because the intended names were already assigned.",
  XLIFF_IMPORT_RESULT_DUPLICATE_NAME_singular_text:
    "A content item has been renamed because the intended name was already assigned.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE_plural_text:
    "Some content items contain empty translations of whitespace only sources.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE_singular_text:
    "A content item contains an empty translation of a whitespace only source.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_plural_text: "Some content items contain empty translations.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_singular_text: "A content item contains an empty translation.",
  XLIFF_IMPORT_RESULT_FAILED_plural_text: "Some content items could not be imported.",
  XLIFF_IMPORT_RESULT_FAILED_singular_text: "A content could not be imported.",
  XLIFF_IMPORT_RESULT_INVALID_CONTENT_ID_plural_text:
    "Some content items could not be imported because they contain invalid IDs.",
  XLIFF_IMPORT_RESULT_INVALID_CONTENT_ID_singular_text:
    "A content item could not be imported because it contains an invalid ID.",
  XLIFF_IMPORT_RESULT_INVALID_INTERNAL_LINK_plural_text:
    "Some content items contain internal links that could not be resolved properly. Using links from master content items.",
  XLIFF_IMPORT_RESULT_INVALID_INTERNAL_LINK_singular_text:
    "A content item contains links that could not be resolved properly. Using links from master content item.",
  XLIFF_IMPORT_RESULT_INVALID_LOCALE_plural_text: "Some target locales do not match the expected locales.",
  XLIFF_IMPORT_RESULT_INVALID_LOCALE_singular_text: "The target locale does not match the expected locale.",
  XLIFF_IMPORT_RESULT_INVALID_MARKUP_plural_text:
    "Some content items could not be imported because they contain invalid markup.",
  XLIFF_IMPORT_RESULT_INVALID_MARKUP_singular_text:
    "A content item could not be imported because it contains invalid markup.",
  XLIFF_IMPORT_RESULT_INVALID_PROPERTY_TYPE_plural_text:
    "Some content items could not be imported because some properties have wrong types.",
  XLIFF_IMPORT_RESULT_INVALID_PROPERTY_TYPE_singular_text:
    "A content item could not be imported because some properties have wrong types.",
  XLIFF_IMPORT_RESULT_INVALID_XLIFF_plural_text: "Content items could not be imported because the XLIFF is not valid.",
  XLIFF_IMPORT_RESULT_INVALID_XLIFF_singular_text:
    "Content items could not be imported because the XLIFF is not valid.",
  XLIFF_IMPORT_RESULT_LIST_TOO_LONG_plural_text:
    "Some content items could not be imported because lists contain too many items.",
  XLIFF_IMPORT_RESULT_LIST_TOO_LONG_singular_text:
    "A content item could not be imported because lists contain too many items.",
  XLIFF_IMPORT_RESULT_MASTER_CHANGED_plural_text:
    "For some content items new master content items were chosen after the translation process had been started.",
  XLIFF_IMPORT_RESULT_MASTER_CHANGED_singular_text:
    "For a content item a new master content item was chosen after the translation process had been started.",
  XLIFF_IMPORT_RESULT_MASTER_VERSION_OUTDATED_plural_text:
    "More recent versions of content items have already been translated.",
  XLIFF_IMPORT_RESULT_MASTER_VERSION_OUTDATED_singular_text:
    "A more recent version of a content item has already been translated.",
  XLIFF_IMPORT_RESULT_NOT_AUTHORIZED_plural_text: "Insufficient rights to import some content items.",
  XLIFF_IMPORT_RESULT_NOT_AUTHORIZED_singular_text: "Insufficient rights to import a content item.",
  XLIFF_IMPORT_RESULT_NO_SUCH_PROPERTY_plural_text:
    "Some content items could not be imported because they declare properties that do not exist.",
  XLIFF_IMPORT_RESULT_NO_SUCH_PROPERTY_singular_text:
    "A content item could not be imported because it declares properties that do not exist.",
  XLIFF_IMPORT_RESULT_STRING_LIST_TOO_LONG_plural_text:
    "Some content items could not be imported because some string lists contain too many items.",
  XLIFF_IMPORT_RESULT_STRING_LIST_TOO_LONG_singular_text:
    "A content item could not be imported because some string lists contain too many items.",
  XLIFF_IMPORT_RESULT_STRING_TOO_LONG_plural_text:
    "Some content items could not be imported because some values of string properties are too long.",
  XLIFF_IMPORT_RESULT_STRING_TOO_LONG_singular_text:
    "A content item could not be imported because some values of string properties are too long.",
  XLIFF_IMPORT_RESULT_SUCCESS_plural_text: "The translation results have successfully been imported.",
  XLIFF_IMPORT_RESULT_SUCCESS_singular_text: "The translation result has successfully been imported.",
};

export default GccWorkflowLocalization_properties;
