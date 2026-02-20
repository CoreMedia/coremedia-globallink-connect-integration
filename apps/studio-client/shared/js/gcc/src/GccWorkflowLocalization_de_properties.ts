import GccWorkflowLocalization_properties from "./GccWorkflowLocalization_properties";

Object.assign(GccWorkflowLocalization_properties, {
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
  XLIFF_IMPORT_RESULT_CHECKED_OUT_BY_OTHER_USER_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil sie von anderen Nutzern ausgeliehen sind.",
  XLIFF_IMPORT_RESULT_CHECKED_OUT_BY_OTHER_USER_singular_text:
    "Ein Inhalt konnte nicht importiert werden, weil er von einem anderen Nutzer ausgeliehen ist.",
  XLIFF_IMPORT_RESULT_DOES_NOT_EXIST_plural_text: "Einige Inhalte existieren nicht.",
  XLIFF_IMPORT_RESULT_DOES_NOT_EXIST_singular_text: "Ein Inhalt existiert nicht.",
  XLIFF_IMPORT_RESULT_DUPLICATE_NAME_plural_text:
    "Einige Inhalte wurden umbenannt, weil die gewünschten Namen schon vergeben sind.",
  XLIFF_IMPORT_RESULT_DUPLICATE_NAME_singular_text:
    "Ein Inhalt wurde umbenannt, weil der gewünschte Name schon vergeben ist.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE_plural_text:
    "Einige Inhalte enthalten leere Übersetzungen für Quellen, die nur Leerzeichen enthalten.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_FOR_WHITESPACE_SOURCE_singular_text:
    "Ein Inhalt enthält leere Übersetzungen für Quellen, die nur Leerzeichen enthalten.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_plural_text: "Einige Inhalte enthalten leere Übersetzungen.",
  XLIFF_IMPORT_RESULT_EMPTY_TRANSUNIT_TARGET_singular_text: "Ein Inhalt enthält leere Übersetzungen.",
  XLIFF_IMPORT_RESULT_FAILED_plural_text: "Einige Inhalte konnten nicht importiert werden.",
  XLIFF_IMPORT_RESULT_FAILED_singular_text: "Ein Inhalt konnte nicht importiert werden.",
  XLIFF_IMPORT_RESULT_INVALID_CONTENT_ID_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil die IDs ungültig waren.",
  XLIFF_IMPORT_RESULT_INVALID_CONTENT_ID_singular_text:
    "Ein Inhalt konnte nicht importiert werden, weil dessen ID ungültig war.",
  XLIFF_IMPORT_RESULT_INVALID_INTERNAL_LINK_plural_text:
    "Einige Inhalte enthalten Verweise, die nicht korrekt aufgelöst werden konnten.",
  XLIFF_IMPORT_RESULT_INVALID_INTERNAL_LINK_singular_text:
    "Ein Inhalt enthält Verweise, die nicht korrekt aufgelöst werden konnten.",
  XLIFF_IMPORT_RESULT_INVALID_LOCALE_plural_text:
    "Einige Ziellocales stimmen nicht mit den erwarteten Locales überein.",
  XLIFF_IMPORT_RESULT_INVALID_LOCALE_singular_text: "Die Ziellocale stimmt nicht mit der erwarteten Locale überein.",
  XLIFF_IMPORT_RESULT_INVALID_MARKUP_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil einige Markup-Inhalte ungültig waren.",
  XLIFF_IMPORT_RESULT_INVALID_MARKUP_singular_text:
    "Ein Inhalt konnte nicht importiert werden, weil einige Markup-Inhalte ungültig waren.",
  XLIFF_IMPORT_RESULT_INVALID_PROPERTY_TYPE_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil einige zu importierende Eigenschaften vom falschen Typ sind.",
  XLIFF_IMPORT_RESULT_INVALID_PROPERTY_TYPE_singular_text:
    "Ein Inhalt konnte nicht importiert werden, weil einige zu importierende Eigenschaften vom falschen Typ sind.",
  XLIFF_IMPORT_RESULT_INVALID_XLIFF_plural_text:
    "Inhalte konnten nicht importiert werden, weil die zu importierende XLIFF-Datei ungültig ist.",
  XLIFF_IMPORT_RESULT_INVALID_XLIFF_singular_text:
    "Inhalte konnten nicht importiert werden, weil die zu importierende XLIFF-Datei ungültig ist.",
  XLIFF_IMPORT_RESULT_LIST_TOO_LONG_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil einige Listen zu viele Elemente enthalten.",
  XLIFF_IMPORT_RESULT_LIST_TOO_LONG_singular_text:
    "Ein Inhalt konnten nicht importiert werden, weil einige Listen zu viele Elemente enthalten.",
  XLIFF_IMPORT_RESULT_MASTER_CHANGED_plural_text:
    "Für einige Inhalte wurden neue Master-Inhalte gewählt, nachdem die Übersetzung gestartet wurde.",
  XLIFF_IMPORT_RESULT_MASTER_CHANGED_singular_text:
    "Für einen Inhalt wurde ein neuer Master-Inhalt gewählt, nachdem die Übersetzung gestartet wurde.",
  XLIFF_IMPORT_RESULT_MASTER_VERSION_OUTDATED_plural_text: "Neuere Inhaltsversionen wurden bereits übersetzt.",
  XLIFF_IMPORT_RESULT_MASTER_VERSION_OUTDATED_singular_text: "Eine neuere Inhaltsversion wurde bereits übersetzt.",
  XLIFF_IMPORT_RESULT_NOT_AUTHORIZED_plural_text:
    "Einige Inhalte konnten wegen fehlender Berechtigungen nicht importiert werden.",
  XLIFF_IMPORT_RESULT_NOT_AUTHORIZED_singular_text:
    "Ein Inhalt konnte wegen fehlender Berechtigungen nicht importiert werden.",
  XLIFF_IMPORT_RESULT_NO_SUCH_PROPERTY_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil einige zu importierende Eigenschaften nicht existieren.",
  XLIFF_IMPORT_RESULT_NO_SUCH_PROPERTY_singular_text:
    "Ein Inhalt konnte nicht importiert werden, weil einige zu importierende Eigenschaften nicht existieren.",
  XLIFF_IMPORT_RESULT_STRING_LIST_TOO_LONG_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil einige Zeichenkettenlisten zu viele Elemente enthalten.",
  XLIFF_IMPORT_RESULT_STRING_LIST_TOO_LONG_singular_text:
    "Ein Inhalt konnten nicht importiert werden, weil einige Zeichenkettenlisten zu viele Elemente enthalten.",
  XLIFF_IMPORT_RESULT_STRING_TOO_LONG_plural_text:
    "Einige Inhalte konnten nicht importiert werden, weil einige Zeichenketten zu lang sind.",
  XLIFF_IMPORT_RESULT_STRING_TOO_LONG_singular_text:
    "Ein Inhalt konnte nicht importiert werden, weil einige Zeichenketten zu lang sind.",
  XLIFF_IMPORT_RESULT_SUCCESS_plural_text: "Die Übersetzungsergebnisse wurden erfolgreich importiert.",
  XLIFF_IMPORT_RESULT_SUCCESS_singular_text: "Das Übersetzungsergebnis wurde erfolgreich importiert.",
});
