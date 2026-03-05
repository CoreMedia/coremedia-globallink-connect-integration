interface Gcc_properties {
  TranslationGlobalLink_submission_id_key: string;
  TranslationGlobalLink_submission_status_key: string;
  TranslationGlobalLink_submission_dueDate_key: string;
  TranslationGlobalLink_submission_id_unavailable: string;
  TranslationGlobalLink_submission_dueDate_tooltip: string;
  TranslationGlobalLink_submission_status_IN_PRE_PROCESS: string;
  TranslationGlobalLink_submission_status_STARTED: string;
  TranslationGlobalLink_submission_status_ANALYZED: string;
  TranslationGlobalLink_submission_status_AWAITING_APPROVAL: string;
  TranslationGlobalLink_submission_status_AWAITING_QUOTE_APPROVAL: string;
  TranslationGlobalLink_submission_status_IN_PROGRESS: string;
  TranslationGlobalLink_submission_status_CANCELLED: string;
  TranslationGlobalLink_submission_status_CANCELLATION_CONFIRMED: string;
  TranslationGlobalLink_submission_status_CANCELLATION_REQUESTED: string;
  TranslationGlobalLink_submission_status_TRANSLATE: string;
  TranslationGlobalLink_submission_status_REVIEW: string;
  TranslationGlobalLink_submission_status_COMPLETED: string;
  TranslationGlobalLink_submission_status_REDELIVERED: string;
  TranslationGlobalLink_submission_status_DELIVERED: string;
  TranslationGlobalLink_submission_status_OTHER: string;
  TranslationGlobalLink_submission_status_unavailable: string;

  translationResultXliff_Button_text: string;
  translationResultXliff_Label_Button_text: string;
  confirm_cancellation_title: string;
  confirm_cancellation: string;
  Action_text_Cancel_Process: string;
  Action_tooltip_Cancel_Process: string;
  TranslationGlobalLink_completed_Locales: string;
  TranslationGlobalLink_Multi_Target_Locale_Text: string;
}

const Gcc_properties: Gcc_properties = {
  TranslationGlobalLink_submission_id_key: "Submission ID",
  TranslationGlobalLink_submission_status_key: "Status",
  TranslationGlobalLink_submission_dueDate_key: "Due Date",
  TranslationGlobalLink_submission_id_unavailable: "Not available yet",
  TranslationGlobalLink_submission_dueDate_tooltip: "Please choose future date.",
  TranslationGlobalLink_submission_status_IN_PRE_PROCESS: "In preprocess",
  TranslationGlobalLink_submission_status_STARTED: "Started",
  TranslationGlobalLink_submission_status_ANALYZED: "Analyzed",
  TranslationGlobalLink_submission_status_AWAITING_APPROVAL: "Awaiting Approval",
  TranslationGlobalLink_submission_status_AWAITING_QUOTE_APPROVAL: "Awaiting Quote Approval",
  TranslationGlobalLink_submission_status_IN_PROGRESS: "In Progress",
  TranslationGlobalLink_submission_status_CANCELLED: "Canceled",
  TranslationGlobalLink_submission_status_CANCELLATION_CONFIRMED: "Canceled",
  TranslationGlobalLink_submission_status_CANCELLATION_REQUESTED: "Canceled",
  TranslationGlobalLink_submission_status_TRANSLATE: "Translate",
  TranslationGlobalLink_submission_status_REVIEW: "Review",
  TranslationGlobalLink_submission_status_COMPLETED: "Completed",
  TranslationGlobalLink_submission_status_REDELIVERED: "Redelivered",
  TranslationGlobalLink_submission_status_DELIVERED: "Delivered",
  TranslationGlobalLink_submission_status_OTHER: "Unknown",
  TranslationGlobalLink_submission_status_unavailable: "Unavailable",
  translationResultXliff_Button_text: "Click to download",
  translationResultXliff_Label_Button_text: "Issue Details",
  confirm_cancellation_title: "Cancel Submission",
  confirm_cancellation: "This action will cancel the submission(s) at GlobalLink. Do you really want to proceed?",
  Action_text_Cancel_Process: "Cancel",
  Action_tooltip_Cancel_Process: "Cancel the selected translation(s) with GlobalLink",
  TranslationGlobalLink_completed_Locales: "Completed Locales",
  TranslationGlobalLink_Multi_Target_Locale_Text: "{0} Locales",
};

export default Gcc_properties;
