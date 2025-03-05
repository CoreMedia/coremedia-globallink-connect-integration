import ResourceBundleUtil from "@jangaroo/runtime/l10n/ResourceBundleUtil";
import GccWorkflowLocalization_properties from "./GccWorkflowLocalization_properties";

ResourceBundleUtil.override(GccWorkflowLocalization_properties, {
  TranslationGlobalLink_displayName: "Übersetzung mit GlobalLink",
  TranslationGlobalLink_description: "Übersetzung mit GlobalLink",

  TranslationGlobalLink_task_Prepare_displayName: "Vorbereitung",
  TranslationGlobalLink_task_AutoMerge_displayName: "Aktualisierung nicht zu übersetzender Felder",
  TranslationGlobalLink_task_SendTranslationRequest_displayName: "Senden der Übersetzungsanfrage",
  TranslationGlobalLink_task_CancelTranslation_displayName: "Warten auf Abbruch-Verarbeitung",
  TranslationGlobalLink_task_DownloadTranslation_displayName: "Warten auf Übersetzung",
  TranslationGlobalLink_task_ReviewDeliveredTranslation_displayName: "Übersetzung prüfen",
  TranslationGlobalLink_task_ReviewRedeliveredTranslation_displayName: "Übersetzung prüfen (erneut ausgeliefert)",
  TranslationGlobalLink_task_ReviewCancelledTranslation_displayName: "Abbruch prüfen",
  TranslationGlobalLink_task_RollbackContent_displayName: "Verwerfen der Änderungen",
  TranslationGlobalLink_task_Complete_displayName: "Aktualisierung des Status der abgeleiteten Inhalte",
  TranslationGlobalLink_task_Finish_displayName: "Archivierung des Workflows",
  TranslationGlobalLink_task_HandleSendTranslationRequestError_displayName: "Fehler beim Senden",
  TranslationGlobalLink_task_HandleDownloadTranslationError_displayName: "Fehler beim Empfangen",
  TranslationGlobalLink_task_HandleCancelTranslationError_displayName: "Fehler beim Abbrechen",

  TranslationGlobalLink_state_Translate_displayName: "Übersetzungs-Workflow annehmen",
  TranslationGlobalLink_state_rollbackTranslation_displayName: "Beenden und Änderungen verwerfen",
  TranslationGlobalLink_state_rollbackTranslation_afterCancellationFailed_displayName:
    "Änderungen verwerfen ohne Abbruch des GlobalLink-Auftrags",
  TranslationGlobalLink_state_finishTranslation_displayName: "Lokalisierung abschließen",
  TranslationGlobalLink_state_DownloadTranslation_displayName: "Warten auf Übersetzung",
  TranslationGlobalLink_state_ReviewDeliveredTranslation_displayName: "Übersetzung prüfen",
  TranslationGlobalLink_state_ReviewRedeliveredTranslation_displayName: "Übersetzung prüfen (erneut ausgeliefert)",
  TranslationGlobalLink_state_ReviewCancelledTranslation_displayName: "Abbruch prüfen",
  TranslationGlobalLink_state_translationReviewed_displayName: "Lokalisierung abschließen (Übersetzung geprüft)",
  TranslationGlobalLink_state_continueRetry_displayName: "Weiter, und erneut versuchen",
  TranslationGlobalLink_state_retryCancellation_displayName: "Abbrechen erneut versuchen",
  TranslationGlobalLink_state_continueTranslation_displayName: "Lokalisierung fortsetzen",
  TranslationGlobalLink_state_HandleSendTranslationRequestError_displayName: "Fehler beim Senden",
  TranslationGlobalLink_state_HandleDownloadTranslationError_displayName: "Fehler beim Empfangen",
  TranslationGlobalLink_state_HandleCancelTranslationError_displayName: "Fehler beim Abbrechen",

  "GCC-WF-10000_text": "Ein unerwarteter Fehler ist aufgetreten.",
  "GCC-WF-20000_text": "Bei der Kommunikation mit GlobalLink ist ein Fehler aufgetreten.",
  "GCC-WF-30001_text": "Ein lokaler Ein-/Ausgabefehler ist aufgetreten.",
  "GCC-WF-40000_text": "Die Konfiguration der GlobalLink-Verbindung ist fehlerhaft.",
  "GCC-WF-40001_text":
    "Die Konfiguration der GlobalLink-Verbindung ist fehlerhaft. Der Wert für 'fileType' wird nicht von GlobalLink unterstützt.",
  "GCC-WF-40002_text":
    "Die Verbindung zu GlobalLink ist aufgrund eines abgelaufenen oder fehlerhaften 'apiKey' fehlgeschlagen.",
  "GCC-WF-40003_text":
    "Der angegebene Connector Key in der Konfiguration der GlobalLink-Verbindung ist ungültig (Eigenschaft 'key').",
  "GCC-WF-40050_text": "Die GlobalLink-Auftragsnummer ist fehlerhaft.",
  "GCC-WF-50050_text": "Der XLIFF-Export ist fehlgeschlagen.",
  "GCC-WF-60000_text": "Allgemeiner GlobalLink-Auftragsfehler.",
  "GCC-WF-60001_text":
    "GlobalLink-Auftrag nicht gefunden. Die Konfiguration der GlobalLink-Verbindung könnte fehlerhaft sein.",
  "GCC-WF-61001_text": "Das Abbrechen des GlobalLink-Auftrags ist fehlgeschlagen.",
  dateLiesInPast_globalLinkDueDate_text: "Bitte Fälligkeitsdatum in der Zukunft auswählen.",
  dateInvalid_globalLinkDueDate_text: "Bitte gültiges Fälligkeitsdatum eingeben.",
  SUCCESS_singular_text: "Das Übersetzungsergebnis wurde erfolgreich importiert.",
  SUCCESS_plural_text: "Die Übersetzungsergebnisse wurden erfolgreich importiert.",
});
