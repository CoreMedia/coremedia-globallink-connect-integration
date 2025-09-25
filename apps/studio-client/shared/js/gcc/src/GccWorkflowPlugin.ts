import { Process, session, Task, WorkflowObjectProperties } from "@coremedia/studio-client.cap-rest-client";
import { Blob, Calendar, RemoteBean, RemoteBeanUtil } from "@coremedia/studio-client.client-core";
import {
  Binding,
  Button,
  DateTimeField,
  RunningWorkflowFormExtension,
  StartWorkflowFormExtension,
  TextField,
  TranslationWorkflowPlugin,
  WorkflowIssuesLocalization,
  WorkflowLocalization,
  workflowLocalizationRegistry,
  workflowPlugins,
  WorkflowState,
} from "@coremedia/studio-client.workflow-plugin-models";
import { as, is, joo } from "@jangaroo/runtime";
import { getLocalizer, registerLocale } from "@coremedia/studio-client.i18n-models";
import { BlobUtil } from "@coremedia/studio-client.cap-base-models";
import GccWorkflowLocalization_properties from "./GccWorkflowLocalization_properties";
import Gcc_properties from "./Gcc_properties";
import gccCanceledIcon from "./icons/global-link-workflow-canceled.svg";
import gccIcon from "./icons/global-link-workflow.svg";
import gccCancelActionIcon from "./icons/remove.svg";
import gccWarningIcon from "./icons/warning.svg";
import { translationServicesSettings } from "./TranslationServiceSettings";

registerLocale(Gcc_properties, "de", async () => {
  await import("./Gcc_de_properties");
});
registerLocale(Gcc_properties, "ja", async () => {
  await import("./Gcc_ja_properties");
});

registerLocale(GccWorkflowLocalization_properties, "de", async () => {
  await import("./GccWorkflowLocalization_de_properties");
});

registerLocale(GccWorkflowLocalization_properties, "ja", async () => {
  await import("./GccWorkflowLocalization_ja_properties");
});

const UNAVAILABLE_SUBMISSION_STATE: string = "unavailable";
const BLOB_FILE_PROCESS_VARIABLE_NAME: string = "translationResultXliff";
const TRANSLATION_RESULT_XLIFF_VARIABLE_NAME: string = "translationResultXliff";
/**
 * Task names where the XLIFF download should be shown. An additional check is
 * applied, which is, that if the XLIFF download is not available, the button
 * is hidden, too.
 */
const SHOW_XLIFF_DOWNLOAD_TASK_NAMES: string[] = ["HandleDownloadTranslationError", "ReviewRedeliveredTranslation"];
const CANCEL_REQUESTED_VARIABLE_NAME: string = "cancelRequested";
const CANCELLATION_ALLOWED_VARIABLE_NAME: string = "cancellationAllowed";
const TRANSLATION_GLOBAL_LINK_PROCESS_NAME: string = "TranslationGlobalLink";
const GLOBAL_LINK_SUBMISSION_STATUS_VARIABLE_NAME: string = "globalLinkSubmissionStatus";
/**
 * Virtual state for a translation, where the state itself does not yet
 * represent the canceled state, but it is only received via an extra
 * property. Thus, the state could still be `TRANSLATE` while the property
 * to cancel the translation already has been set.
 */
const CANCELLATION_REQUESTED_SUBMISSION_STATE = "CANCELLATION_REQUESTED";
const CANCELLATION_CONFIRMED_SUBMISSION_STATE: string = "CANCELLATION_CONFIRMED";
const CANCELLED_SUBMISSION_STATE: string = "CANCELLED";
const HANDLE_SEND_TRANSLATION_REQUEST_ERROR_TASK_NAME: string = "HandleSendTranslationRequestError";
const HANDLE_DOWNLOAD_TRANSLATION_ERROR_TASK_NAME: string = "HandleDownloadTranslationError";
const HANDLE_CANCEL_TRANSLATION_ERROR_TASK_NAME: string = "HandleCancelTranslationError";
const MILLISECONDS_FOR_ONE_DAY: number = 86400000;

/**
 * Format options to display the due date in the workflow UI.
 *
 * **Example:**
 *
 * * en-US: 09/25/2025, 06:05 PM GMT+2
 * * de-DE: 25.09.2025, 18:05 MESZ
 */
const dateTimeFormat: Intl.DateTimeFormatOptions = {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  timeZoneName: "short",
};

interface GccViewModel {
  globalLinkPdSubmissionIds?: string;
  globalLinkSubmissionStatus?: string;
  submissionStatusHidden?: boolean;
  globalLinkDueDate?: Date;
  globalLinkDueCalendar?: Calendar;
  globalLinkDueDateText?: string;
  completedLocales?: string;
  completedLocalesTooltip?: string;
  xliffResultDownloadNotAvailable?: boolean;
}

/**
 * Retrieve the submission status from properties. Respects a virtual state
 * `CANCEL` when the status itself does not yet represent any state that is
 * part of the cancelation process. Thus, the state may still be _Translate_
 * while an additional property already denotes, that we requested to cancel
 * the translation.
 *
 * @param process - process to get state for
 */
const getSubmissionStatus = (process): string => {
  const properties = process.getProperties();
  const gccSubmissionsState = properties.get(GLOBAL_LINK_SUBMISSION_STATUS_VARIABLE_NAME) as string;
  if ([CANCELLATION_CONFIRMED_SUBMISSION_STATE, CANCELLED_SUBMISSION_STATE].includes(gccSubmissionsState)) {
    // No need to override. These are well-known states of the cancelation process.
    return gccSubmissionsState;
  }
  const cancelRequested = properties.get(CANCEL_REQUESTED_VARIABLE_NAME) as boolean;
  if (cancelRequested) {
    // Override and use virtual state.
    return CANCELLATION_REQUESTED_SUBMISSION_STATE;
  }
  return gccSubmissionsState;
};

const getGccWorkflowPlugin = async (): Promise<TranslationWorkflowPlugin> => {
  const localizer = await getLocalizer(Gcc_properties);

  return {
    workflowType: "TRANSLATION",

    workflowName: "TranslationGlobalLink",

    createWorkflowPerTargetSite: false,

    customizeWorkflowIcon: (process, task?) => {
      if (
        !!task &&
        (!task.getWarnings() || task.getWarnings().length === 0) &&
        !task.isEscalated() &&
        !task.isAccepted() &&
        (task.getDefinition().getName() === HANDLE_SEND_TRANSLATION_REQUEST_ERROR_TASK_NAME ||
          task.getDefinition().getName() === HANDLE_DOWNLOAD_TRANSLATION_ERROR_TASK_NAME ||
          task.getDefinition().getName() === HANDLE_CANCEL_TRANSLATION_ERROR_TASK_NAME)
      ) {
        return gccWarningIcon;
      }

      const status = getSubmissionStatus(process);
      if (
        [
          CANCELLATION_REQUESTED_SUBMISSION_STATE,
          CANCELLATION_CONFIRMED_SUBMISSION_STATE,
          CANCELLED_SUBMISSION_STATE,
        ].includes(status)
      ) {
        return gccCanceledIcon;
      }

      return gccIcon;
    },

    nextStepVariable: "translationAction",

    transitions: [
      {
        task: "ReviewDeliveredTranslation",
        defaultNextTask: "finishTranslation",
        nextSteps: [
          {
            name: "rollbackTranslation",
            allowAlways: true,
          },
          {
            name: "finishTranslation",
            allowAlways: true,
          },
        ],
      },
      {
        task: "ReviewRedeliveredTranslation",
        defaultNextTask: "finishTranslation",
        nextSteps: [
          {
            name: "rollbackTranslation",
            allowAlways: true,
          },
          {
            name: "finishTranslation",
            allowAlways: true,
          },
        ],
      },
      {
        task: "ReviewCancelledTranslation",
        defaultNextTask: "rollbackTranslation",
        nextSteps: [
          {
            name: "rollbackTranslation",
            allowAlways: true,
          },
        ],
      },
      {
        task: "HandleSendTranslationRequestError",
        defaultNextTask: "rollbackTranslation",
        nextSteps: [
          {
            name: "rollbackTranslation",
            allowAlways: true,
          },
          {
            name: "continueRetry",
            allowAlways: true,
          },
        ],
      },
      {
        task: "HandleDownloadTranslationError",
        defaultNextTask: "rollbackTranslation",
        nextSteps: [
          {
            name: "rollbackTranslation",
            allowAlways: true,
          },
          {
            name: "continueRetry",
            allowAlways: true,
          },
        ],
      },
      {
        task: "HandleCancelTranslationError",
        defaultNextTask: "rollbackTranslation",
        nextSteps: [
          {
            name: "rollbackTranslation_afterCancellationFailed",
            allowAlways: true,
          },
          {
            name: "retryCancellation",
            allowAlways: true,
          },
          {
            name: "continueTranslation",
            allowAlways: true,
          },
        ],
      },
    ],
    startWorkflowFormExtension: StartWorkflowFormExtension<GccViewModel>({
      computeViewModel() {
        const dueDate = getDefaultDueDate();
        if (!dueDate) {
          return undefined;
        }

        return { globalLinkDueCalendar: dueDate };
      },

      saveViewModel(viewModel: GccViewModel): Record<string, unknown> {
        return { globalLinkDueDate: viewModel.globalLinkDueCalendar };
      },

      remotelyValidatedViewModelFields: ["globalLinkDueCalendar"],

      fields: [
        DateTimeField({
          label: localizer("TranslationGlobalLink_submission_dueDate_key"),
          tooltip: localizer("TranslationGlobalLink_submission_dueDate_tooltip"),
          value: Binding("globalLinkDueCalendar"),
        }),
      ],
    }),

    runningWorkflowFormExtension: RunningWorkflowFormExtension<GccViewModel>({
      computeTaskFromProcess: (process: Process): Task => process.getCurrentTask(),
      computeViewModel(state: WorkflowState): GccViewModel {
        return {
          globalLinkPdSubmissionIds: transformSubmissionId(
            state.process.getProperties().get("globalLinkPdSubmissionIds"),
          ),
          globalLinkSubmissionStatus: transformSubmissionStatus(getSubmissionStatus(state.process)),
          globalLinkDueDate: dateToDate(state.process.getProperties().get("globalLinkDueDate")),
          globalLinkDueCalendar: state.process.getProperties().get("globalLinkDueDate") || undefined,
          globalLinkDueDateText: dateToString(state.process.getProperties().get("globalLinkDueDate")),
          completedLocales: convertLocales(localizer, state.process.getProperties().get("completedLocales")),
          completedLocalesTooltip: createQuickTipText(state.process.getProperties().get("completedLocales")),
          xliffResultDownloadNotAvailable: downloadNotAvailable(state.task),
        };
      },

      saveViewModel() {
        return {};
      },

      remotelyValidatedViewModelFields: ["globalLinkDueCalendar"],

      fields: [
        TextField({
          label: localizer("TranslationGlobalLink_submission_id_key"),
          value: Binding("globalLinkPdSubmissionIds"),
          readonly: true,
        }),
        TextField({
          label: localizer("TranslationGlobalLink_submission_status_key"),
          value: Binding("globalLinkSubmissionStatus"),
          readonly: true,
        }),
        TextField({
          label: localizer("TranslationGlobalLink_submission_dueDate_key"),
          readonly: true,
          value: Binding("globalLinkDueDateText"),
        }),
        TextField({
          label: localizer("TranslationGlobalLink_completed_Locales"),
          readonly: true,
          value: Binding("completedLocales"),
          tooltip: Binding("completedLocalesTooltip"),
        }),
        Button({
          label: localizer("translationResultXliff_Label_Button_text"),
          value: localizer("translationResultXliff_Button_text"),
          validationState: "error",
          handler: (state): GccViewModel | void => downloadXliff(state.task),
          hidden: Binding("xliffResultDownloadNotAvailable"),
        }),
      ],
    }),

    workflowListActions: [
      {
        text: localizer("Action_text_Cancel_Process"),
        tooltip: localizer("Action_tooltip_Cancel_Process"),
        svgIcon: gccCancelActionIcon,
        handler: (workflowObjects): void => {
          const processes: Array<Process> = <Array<Process>>workflowObjects;
          processes.forEach((po: Process): void => {
            po.getProperties().set(CANCEL_REQUESTED_VARIABLE_NAME, true);
          });
        },
        confirmTitle: localizer("confirm_cancellation_title"),
        confirmMessage: localizer("confirm_cancellation"),
        computeActionState: (workflowObjects) => {
          if (!workflowObjects || workflowObjects.length === 0) {
            return {
              hidden: true,
              disabled: true,
            };
          }

          for (let i: number = 0; i < workflowObjects.length; i++) {
            const wfobject = workflowObjects[i];

            if (
              !is(wfobject, Process) ||
              !RemoteBeanUtil.isAccessible(wfobject) ||
              wfobject.getDefinition().getName() !== TRANSLATION_GLOBAL_LINK_PROCESS_NAME
            ) {
              return {
                hidden: true,
                disabled: true,
              };
            }
            if (
              !wfobject.getProperties().get(CANCELLATION_ALLOWED_VARIABLE_NAME) ||
              wfobject.getProperties().get(CANCEL_REQUESTED_VARIABLE_NAME)
            ) {
              return {
                hidden: false,
                disabled: true,
              };
            }
          }

          return {
            hidden: false,
            disabled: false,
          };
        },
      },
    ],
  };
};

getGccWorkflowPlugin().then((gccWorkflowPlugin) => {
  workflowPlugins._.addTranslationWorkflowPlugin(gccWorkflowPlugin);
});

const getGccProcessLocalization = async (): Promise<WorkflowLocalization> => {
  const localizer = await getLocalizer(GccWorkflowLocalization_properties);

  return {
    displayName: localizer("TranslationGlobalLink_displayName"),
    description: localizer("TranslationGlobalLink_description"),
    svgIcon: gccIcon,
    states: {
      Translate: localizer("TranslationGlobalLink_state_Translate_displayName"),
      rollbackTranslation: localizer("TranslationGlobalLink_state_rollbackTranslation_displayName"),
      rollbackTranslation_afterCancellationFailed: localizer(
        "TranslationGlobalLink_state_rollbackTranslation_afterCancellationFailed_displayName",
      ),
      finishTranslation: localizer("TranslationGlobalLink_state_finishTranslation_displayName"),
      DownloadTranslation: localizer("TranslationGlobalLink_state_DownloadTranslation_displayName"),
      ReviewDeliveredTranslation: localizer("TranslationGlobalLink_state_ReviewDeliveredTranslation_displayName"),
      ReviewRedeliveredTranslation: localizer("TranslationGlobalLink_state_ReviewRedeliveredTranslation_displayName"),
      ReviewCancelledTranslation: localizer("TranslationGlobalLink_state_ReviewCancelledTranslation_displayName"),
      translationReviewed: localizer("TranslationGlobalLink_state_translationReviewed_displayName"),
      continueRetry: localizer("TranslationGlobalLink_state_continueRetry_displayName"),
      retryCancellation: localizer("TranslationGlobalLink_state_retryCancellation_displayName"),
      continueTranslation: localizer("TranslationGlobalLink_state_continueTranslation_displayName"),
      HandleSendTranslationRequestError: localizer(
        "TranslationGlobalLink_state_HandleSendTranslationRequestError_displayName",
      ),
      HandleDownloadTranslationError: localizer(
        "TranslationGlobalLink_state_HandleDownloadTranslationError_displayName",
      ),
      HandleCancelTranslationError: localizer("TranslationGlobalLink_state_HandleCancelTranslationError_displayName"),
    },
    tasks: {
      Prepare: localizer("TranslationGlobalLink_task_Prepare_displayName"),
      AutoMerge: localizer("TranslationGlobalLink_task_AutoMerge_displayName"),
      SendTranslationRequest: localizer("TranslationGlobalLink_task_SendTranslationRequest_displayName"),
      CancelTranslation: localizer("TranslationGlobalLink_task_CancelTranslation_displayName"),
      DownloadTranslation: localizer("TranslationGlobalLink_task_DownloadTranslation_displayName"),
      ReviewDeliveredTranslation: localizer("TranslationGlobalLink_task_ReviewDeliveredTranslation_displayName"),
      ReviewRedeliveredTranslation: localizer("TranslationGlobalLink_task_ReviewRedeliveredTranslation_displayName"),
      ReviewCancelledTranslation: localizer("TranslationGlobalLink_task_ReviewCancelledTranslation_displayName"),
      RollbackContent: localizer("TranslationGlobalLink_task_RollbackContent_displayName"),
      Complete: localizer("TranslationGlobalLink_task_Complete_displayName"),
      Finish: localizer("TranslationGlobalLink_task_Finish_displayName"),
      HandleSendTranslationRequestError: localizer(
        "TranslationGlobalLink_task_HandleSendTranslationRequestError_displayName",
      ),
      HandleDownloadTranslationError: localizer(
        "TranslationGlobalLink_task_HandleDownloadTranslationError_displayName",
      ),
      HandleCancelTranslationError: localizer("TranslationGlobalLink_task_HandleCancelTranslationError_displayName"),
    },
  };
};

getGccProcessLocalization().then((gccProcessLocalization) => {
  workflowLocalizationRegistry._.addLocalization("TranslationGlobalLink", gccProcessLocalization);
});

const getGccIssuesLocalization = async (): Promise<WorkflowIssuesLocalization> => {
  const localizer = await getLocalizer(GccWorkflowLocalization_properties);

  return {
    "GCC-WF-10000": localizer("GCC-WF-10000_text"),
    "GCC-WF-20000": localizer("GCC-WF-20000_text"),
    "GCC-WF-30001": localizer("GCC-WF-30001_text"),
    "GCC-WF-40000": localizer("GCC-WF-40000_text"),
    "GCC-WF-40001": localizer("GCC-WF-40001_text"),
    "GCC-WF-40002": localizer("GCC-WF-40002_text"),
    "GCC-WF-40003": localizer("GCC-WF-40003_text"),
    "GCC-WF-40050": localizer("GCC-WF-40050_text"),
    "GCC-WF-50050": localizer("GCC-WF-50050_text"),
    "GCC-WF-60000": localizer("GCC-WF-60000_text"),
    "GCC-WF-60001": localizer("GCC-WF-60001_text"),
    "GCC-WF-61001": localizer("GCC-WF-61001_text"),
    dateLiesInPast_globalLinkDueDate: localizer("dateLiesInPast_globalLinkDueDate_text"),
    dateInvalid_globalLinkDueDate: localizer("dateInvalid_globalLinkDueDate_text"),
    SUCCESS: {
      singular: localizer("SUCCESS_singular_text"),
      plural: localizer("SUCCESS_plural_text"),
    },
  };
};

getGccIssuesLocalization().then((gccIssuesLocalization) => {
  workflowLocalizationRegistry._.addIssuesLocalization(gccIssuesLocalization);
});

function transformSubmissionId(value: Array<string>): string {
  if (value && value.length) {
    return value.join(", ");
  }
  return Gcc_properties.TranslationGlobalLink_submission_id_unavailable;
}

function transformSubmissionStatus(value: string): string {
  if (!value) {
    value = UNAVAILABLE_SUBMISSION_STATE;
  }
  let status = Gcc_properties["TranslationGlobalLink_submission_status_" + value];
  if (!status) {
    status = value;
  }
  return status;
}

function dateToDate(value): Date {
  let date: Date;
  if (is(value, Date)) {
    date = value;
  } else if (is(value, Calendar)) {
    date = value.getDate();
  } else {
    return null;
  }
  return date;
}

function dateToString(value): string {
  let date: Date;
  if (is(value, Date)) {
    date = value;
  } else if (is(value, Calendar)) {
    date = value.getDate();
  } else {
    return null;
  }

  if (date) {
    const locale = joo.localeSupport.getLocale();
    return new Intl.DateTimeFormat(locale, dateTimeFormat).format(date);
  }
}

function downloadXliff(task: Task): void {
  const process = task.getContainingProcess();
  const blob = as(process.getProperties().get(BLOB_FILE_PROCESS_VARIABLE_NAME), Blob);
  const blobUri = blob.getUri();
  const filename = buildFilename(process);
  const withFilenameUrl = BlobUtil.withFilename(blobUri, filename);
  window.open(withFilenameUrl);
}

// No need to provide extension. This will be added automatically on server.
function buildFilename(process: Process): string {
  const subject = process.getProperties().get("subject");
  const dType = process.getDefinition().getName();
  return `${subject}_${dType}`;
}

function downloadNotAvailable(task: Task): boolean {
  if (
    !task ||
    !RemoteBeanUtil.isAccessible(task) ||
    !RemoteBeanUtil.isAccessible(task.getDefinition() as unknown as RemoteBean) ||
    !RemoteBeanUtil.isAccessible(task.getContainingProcess()) ||
    !SHOW_XLIFF_DOWNLOAD_TASK_NAMES.includes(task.getDefinition().getName())
  ) {
    return true;
  }

  const properties: WorkflowObjectProperties =
    task.getContainingProcess() && task.getContainingProcess().getProperties();
  const downloadAvailable: Blob = properties && as(properties.get(TRANSLATION_RESULT_XLIFF_VARIABLE_NAME), Blob);
  return !downloadAvailable;
}

function convertLocales(
  localizer: (key: keyof Gcc_properties, ...vars: string[]) => string,
  locales: Array<string>,
): string {
  if (locales) {
    if (locales.length === 1) {
      const displayNames = new Intl.DisplayNames([joo.localeSupport.getLocale()], { type: "language" });
      return displayNames.of(locales[0]) ?? locales[0];
    }
    const localized = localizer("TranslationGlobalLink_Multi_Target_Locale_Text");
    return localized.replace("{0}", "" + locales.length);
  }
  return "";
}

function createQuickTipText(locales: Array<string>): string {
  let localeQuickTipText = "";
  locales.forEach((localeString: string): void => {
    const displayNames = new Intl.DisplayNames([joo.localeSupport.getLocale()], { type: "language" });
    const displayName = displayNames.of(localeString) ?? localeString;
    if (localeQuickTipText == "") {
      localeQuickTipText = displayName;
    } else {
      localeQuickTipText = localeQuickTipText + ", " + displayName;
    }
  });

  return localeQuickTipText;
}

function getDefaultDueDate(): Calendar | undefined {
  const dayOffsetForDueDate = translationServicesSettings.getDayOffsetForDueDate();
  if (dayOffsetForDueDate === undefined) {
    return undefined;
  }

  const dateInFutureInMillieSeconds = new Date().getTime() + MILLISECONDS_FOR_ONE_DAY * dayOffsetForDueDate;
  const dateInFuture = new Date(dateInFutureInMillieSeconds);
  return new Calendar({
    year: dateInFuture.getFullYear(),
    month: dateInFuture.getMonth(),
    day: dateInFuture.getDate(),
    offset: -dateInFuture.getTimezoneOffset() * (60 * 1000),
    timeZone: session._.getConnection().getContentRepository().getDefaultTimeZone(),
    normalized: true,
  });
}
