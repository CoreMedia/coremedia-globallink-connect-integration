import ILocalesService from "@coremedia/studio-client.cap-base-models/locale/ILocalesService";
import localesService from "@coremedia/studio-client.cap-base-models/locale/localesService";
import ContentRepositoryImpl from "@coremedia/studio-client.cap-rest-client-impl/content/impl/ContentRepositoryImpl";
import TaskDefinitionImpl from "@coremedia/studio-client.cap-rest-client-impl/workflow/impl/TaskDefinitionImpl";
import session from "@coremedia/studio-client.cap-rest-client/common/session";
import Content from "@coremedia/studio-client.cap-rest-client/content/Content";
import Struct from "@coremedia/studio-client.cap-rest-client/struct/Struct";
import StructRemoteBean from "@coremedia/studio-client.cap-rest-client/struct/StructRemoteBean";
import Process from "@coremedia/studio-client.cap-rest-client/workflow/Process";
import Task from "@coremedia/studio-client.cap-rest-client/workflow/Task";
import WorkflowObjectProperties from "@coremedia/studio-client.cap-rest-client/workflow/WorkflowObjectProperties";
import RemoteService from "@coremedia/studio-client.client-core-impl/data/impl/RemoteService";
import Blob from "@coremedia/studio-client.client-core/data/Blob";
import Calendar from "@coremedia/studio-client.client-core/data/Calendar";
import RemoteBeanUtil from "@coremedia/studio-client.client-core/data/RemoteBeanUtil";
import ProcessUtil from "@coremedia/studio-client.workflow-models/util/ProcessUtil";
import { Binding, Button, DateTimeField, TextField, WorkflowState } from "@coremedia/studio-client.workflow-plugin-models/CustomWorkflowApi";
import { workflowLocalizationRegistry } from "@coremedia/studio-client.workflow-plugin-models/WorkflowLocalizationRegistry";
import { workflowPlugins } from "@coremedia/studio-client.workflow-plugin-models/WorkflowPluginRegistry";
import DateUtil from "@jangaroo/ext-ts/Date";
import { as, is } from "@jangaroo/runtime";
import resourceManager from "@jangaroo/runtime/l10n/resourceManager";
import GccWorkflowLocalization_properties from "./GccWorkflowLocalization_properties";
import Gcc_properties from "./Gcc_properties";
import gccCanceledIcon from "./icons/abort-workflow_24.svg";
import gccIcon from "./icons/global-link-workflow_24.svg";
import gccCancelActionIcon from "./icons/remove_24.svg";
import gccWarningIcon from "./icons/warning_24.svg";

const UNAVAILABLE_SUBMISSION_STATE: string = "unavailable";
const BLOB_FILE_PROCESS_VARIABLE_NAME: string = "translationResultXliff";
const TRANSLATION_RESULT_XLIFF_VARIABLE_NAME: string = "translationResultXliff";
const ERROR_TASK_NAME: string = "HandleDownloadTranslationError";
const CANCEL_REQUESTED_VARIABLE_NAME: string = "cancelRequested";
const CANCELLATION_ALLOWED_VARIABLE_NAME: string = "cancellationAllowed";
const TRANSLATION_GLOBAL_LINK_PROCESS_NAME: string = "TranslationGlobalLink";
const GLOBAL_LINK_SUBMISSION_STATUS_VARIABLE_NAME: string = "globalLinkSubmissionStatus";
const CANCELLATION_CONFIRMED_SUBMISSION_STATE: string = "CANCELLATION_CONFIRMED";
const CANCELLED_SUBMISSION_STATE: string = "CANCELLED";
const HANDLE_SEND_TRANSLATION_REQUEST_ERROR_TASK_NAME: string = "HandleSendTranslationRequestError";
const HANDLE_DOWNLOAD_TRANSLATION_ERROR_TASK_NAME: string = "HandleDownloadTranslationError";
const HANDLE_CANCEL_TRANSLATION_ERROR_TASK_NAME: string = "HandleCancelTranslationError";
const MILLISECONDS_FOR_ONE_DAY: number = 86400000;

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

workflowPlugins._.addTranslationWorkflowPlugin<GccViewModel>({
  workflowType: "TRANSLATION",

  workflowName: "TranslationGlobalLink",

  createWorkflowPerTargetSite: false,

  customizeWorkflowIcon: (process, task?) => {
    if (!!task
      && !task.getWarnings()
      && !task.isEscalated()
      && !task.isAccepted()
      && (task.getDefinition().getName() === HANDLE_SEND_TRANSLATION_REQUEST_ERROR_TASK_NAME
        || task.getDefinition().getName() === HANDLE_DOWNLOAD_TRANSLATION_ERROR_TASK_NAME
        || task.getDefinition().getName() === HANDLE_CANCEL_TRANSLATION_ERROR_TASK_NAME)) {
      return gccWarningIcon;
    }

    const gccSubmissionsState: String = process.getProperties().get(GLOBAL_LINK_SUBMISSION_STATUS_VARIABLE_NAME) as String;
    const cancelRequested: Boolean = process.getProperties().get(CANCEL_REQUESTED_VARIABLE_NAME) as Boolean || gccSubmissionsState === CANCELLATION_CONFIRMED_SUBMISSION_STATE || gccSubmissionsState === CANCELLED_SUBMISSION_STATE;
    if (cancelRequested) {
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
          name: "continueRetry",
          allowAlways: true,
        },
      ],
    },
  ],
  startWorkflowFormExtension: {
    computeViewModel() {
      const defaultDueDate = getDefaultDueDate();
      if (!defaultDueDate) {
        return undefined;
      }

      return { globalLinkDueCalendar: defaultDueDate };
    },

    saveViewModel(viewModel: GccViewModel): Record<string, any> {
      return { globalLinkDueDate: viewModel.globalLinkDueCalendar };
    },

    remotelyValidatedViewModelFields: ["globalLinkDueCalendar"],

    fields: [
      DateTimeField({
        label: Gcc_properties.TranslationGlobalLink_submission_dueDate_key,
        tooltip: Gcc_properties.TranslationGlobalLink_submission_dueDate_tooltip,
        value: Binding("globalLinkDueCalendar"),
      }),
    ],
  },

  runningWorkflowFormExtension: {
    computeTaskFromProcess: ProcessUtil.getCurrentTask,
    computeViewModel(state: WorkflowState): GccViewModel {
      return {
        globalLinkPdSubmissionIds: transformSubmissionId(state.process.getProperties().get("globalLinkPdSubmissionIds")),
        globalLinkSubmissionStatus: transformSubmissionStatus(state.process.getProperties().get("globalLinkSubmissionStatus")),
        globalLinkDueDate: dateToDate(state.process.getProperties().get("globalLinkDueDate")),
        globalLinkDueDateText: dateToString(state.process.getProperties().get("globalLinkDueDate")),
        completedLocales: convertLocales(state.process.getProperties().get("completedLocales")),
        completedLocalesTooltip: createQuickTipText(state.process.getProperties().get("completedLocales"), localesService),
        xliffResultDownloadNotAvailable: downloadNotAvailable(state.task),
      };
    },

    saveViewModel(viewModel: GccViewModel) {
      return {
        globalLinkSubmissionStatus: viewModel.globalLinkSubmissionStatus,
        globalLinkDueDate: viewModel.globalLinkDueDate,
      };
    },

    fields: [
      TextField({
        label: Gcc_properties.TranslationGlobalLink_submission_id_key,
        value: Binding("globalLinkPdSubmissionIds"),
        readonly: true,
      }),
      TextField({
        label: Gcc_properties.TranslationGlobalLink_submission_status_key,
        value: Binding("globalLinkSubmissionStatus"),
        readonly: true,
      }),
      TextField({
        label: Gcc_properties.TranslationGlobalLink_submission_dueDate_key,
        readonly: true,
        value: Binding("globalLinkDueDateText"),
      }),
      TextField({
        label: Gcc_properties.TranslationGlobalLink_completed_Locales,
        readonly: true,
        value: Binding("completedLocales"),
        tooltip: Binding("completedLocalesTooltip"),
      }),
      Button({
        label: Gcc_properties.translationResultXliff_Label_Button_text,
        value: Gcc_properties.translationResultXliff_Button_text,
        validationState: "error",
        handler: (state): GccViewModel | void => downloadXliff(state.task),
        hidden: Binding("xliffResultDownloadNotAvailable"),
      }),
    ],
  },

  workflowListActions: [
    {
      text: "Cancel",
      tooltip: Gcc_properties.Action_Tooltip_Cancel_Process,
      svgIcon: gccCancelActionIcon,
      handler: (workflowObjects): void => {
        const processes: Array<Process> = <Array<Process>>workflowObjects;
        processes.forEach((po: Process): void => {
          po.getProperties().set(CANCEL_REQUESTED_VARIABLE_NAME, true);
        });
      },
      confirmTitle: Gcc_properties.confirm_cancellation_title,
      confirmMessage: Gcc_properties.confirm_cancellation,
      computeActionState: workflowObjects => {
        if (!workflowObjects || workflowObjects.length === 0) {
          return {
            hidden: true,
            disabled: true,
          };
        }

        for (let i: number = 0; i < workflowObjects.length; i++) {
          const wfobject = workflowObjects[i];
          if (!is(wfobject, Process)
                  || !RemoteBeanUtil.isAccessible(wfobject)
                  || wfobject.getDefinition().getName() !== TRANSLATION_GLOBAL_LINK_PROCESS_NAME) {
            return {
              hidden: true,
              disabled: true,
            };
          }
          if (!wfobject.getProperties().get(CANCELLATION_ALLOWED_VARIABLE_NAME)
                  || wfobject.getProperties().get(CANCEL_REQUESTED_VARIABLE_NAME)) {
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
});

workflowLocalizationRegistry._.addLocalization("TranslationGlobalLink", {
  displayName: GccWorkflowLocalization_properties.TranslationGlobalLink_displayName,
  description: GccWorkflowLocalization_properties.TranslationGlobalLink_description,
  svgIcon: gccIcon,
  states: {
    Translate: GccWorkflowLocalization_properties.TranslationGlobalLink_state_Translate_displayName,
    rollbackTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_state_rollbackTranslation_displayName,
    rollbackTranslation_afterCancellationFailed: GccWorkflowLocalization_properties.TranslationGlobalLink_state_rollbackTranslation_afterCancellationFailed_displayName,
    finishTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_state_finishTranslation_displayName,
    DownloadTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_state_DownloadTranslation_displayName,
    ReviewDeliveredTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_state_ReviewDeliveredTranslation_displayName,
    ReviewCancelledTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_state_ReviewCancelledTranslation_displayName,
    translationReviewed: GccWorkflowLocalization_properties.TranslationGlobalLink_state_translationReviewed_displayName,
    continueRetry: GccWorkflowLocalization_properties.TranslationGlobalLink_state_continueRetry_displayName,
    HandleSendTranslationRequestError: GccWorkflowLocalization_properties.TranslationGlobalLink_state_HandleSendTranslationRequestError_displayName,
    HandleDownloadTranslationError: GccWorkflowLocalization_properties.TranslationGlobalLink_state_HandleDownloadTranslationError_displayName,
    HandleCancelTranslationError: GccWorkflowLocalization_properties.TranslationGlobalLink_state_HandleCancelTranslationError_displayName,
  },
  tasks: {
    Prepare: GccWorkflowLocalization_properties.TranslationGlobalLink_task_Prepare_displayName,
    AutoMerge: GccWorkflowLocalization_properties.TranslationGlobalLink_task_AutoMerge_displayName,
    SendTranslationRequest: GccWorkflowLocalization_properties.TranslationGlobalLink_task_SendTranslationRequest_displayName,
    DownloadTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_task_DownloadTranslation_displayName,
    ReviewDeliveredTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_task_ReviewDeliveredTranslation_displayName,
    ReviewCancelledTranslation: GccWorkflowLocalization_properties.TranslationGlobalLink_task_ReviewCancelledTranslation_displayName,
    RollbackContent: GccWorkflowLocalization_properties.TranslationGlobalLink_task_RollbackContent_displayName,
    Complete: GccWorkflowLocalization_properties.TranslationGlobalLink_task_Complete_displayName,
    Finish: GccWorkflowLocalization_properties.TranslationGlobalLink_task_Finish_displayName,
    HandleSendTranslationRequestError: GccWorkflowLocalization_properties.TranslationGlobalLink_task_HandleSendTranslationRequestError_displayName,
    HandleDownloadTranslationError: GccWorkflowLocalization_properties.TranslationGlobalLink_task_HandleDownloadTranslationError_displayName,
    HandleCancelTranslationError: GccWorkflowLocalization_properties.TranslationGlobalLink_task_HandleCancelTranslationError_displayName,
  },
});

workflowLocalizationRegistry._.addIssuesLocalization({
  "GCC-WF-10000": GccWorkflowLocalization_properties["GCC-WF-10000_text"],
  "GCC-WF-20000": GccWorkflowLocalization_properties["GCC-WF-20000_text"],
  "GCC-WF-30001": GccWorkflowLocalization_properties["GCC-WF-30001_text"],
  "GCC-WF-40000": GccWorkflowLocalization_properties["GCC-WF-40000_text"],
  "GCC-WF-40001": GccWorkflowLocalization_properties["GCC-WF-40001_text"],
  "GCC-WF-40050": GccWorkflowLocalization_properties["GCC-WF-40050_text"],
  "GCC-WF-50050": GccWorkflowLocalization_properties["GCC-WF-50050_text"],
  "GCC-WF-61001": GccWorkflowLocalization_properties["GCC-WF-61001_text"],
  dateLiesInPast_globalLinkDueDate: GccWorkflowLocalization_properties.dateLiesInPast_globalLinkDueDate_text,
  dateInvalid_globalLinkDueDate: GccWorkflowLocalization_properties.dateInvalid_globalLinkDueDate_text,
  SUCCESS: {
    singular: GccWorkflowLocalization_properties.SUCCESS_singular_text,
    plural: GccWorkflowLocalization_properties.SUCCESS_plural_text,
  },
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
    return DateUtil.format(date, "m/d/Y g:i A");
  }
}

function downloadXliff(task: Task): void {
  const currentDate = new Date();
  const uri = RemoteService.calculateRequestURI("downloadBlob/") +
          "?process=" + task.getContainingProcess().getId() +
          "&blobProcessVariable=" + BLOB_FILE_PROCESS_VARIABLE_NAME +
          "&" + currentDate.getTime();
  window.open(uri);
}

function downloadNotAvailable(task: Task): boolean {
  if (!task
          || !RemoteBeanUtil.isAccessible(task)
          || !RemoteBeanUtil.isAccessible(<TaskDefinitionImpl>task.getDefinition())
          || !RemoteBeanUtil.isAccessible(task.getContainingProcess())
          || task.getDefinition().getName() !== ERROR_TASK_NAME) {
    return true;
  }

  const properties: WorkflowObjectProperties = task.getContainingProcess() && task.getContainingProcess().getProperties();
  const downloadAvailable: Blob = properties && as(properties.get(TRANSLATION_RESULT_XLIFF_VARIABLE_NAME), Blob);
  return !downloadAvailable;
}

function convertLocales(locales: Array<any>): string {
  if (locales) {
    if (locales.length === 1) {
      const locale = localesService.getLocale(locales[0]);
      const displayNameOfLocale: string = locale && locale.getDisplayName();
      return displayNameOfLocale ? displayNameOfLocale : "";
    }
    return resourceManager.getString(Gcc_properties, "TranslationGlobalLink_Multi_Target_Locale_Text", [locales.length]);
  }
  return "";
}

function createQuickTipText(locales: Array<any>, localesService: ILocalesService): string {
  let localeQuickTipText = "";
  locales.forEach((localeString: string): void => {
    const locale = localesService.getLocale(localeString);
    if (localeQuickTipText == "") {
      localeQuickTipText = locale && locale.getDisplayName();
    } else {
      localeQuickTipText = localeQuickTipText + ", " + (locale && locale.getDisplayName());
    }
  });

  return localeQuickTipText;
}

function getDefaultDueDate(): Calendar {
  const gccSettings: Content = session._.getConnection().getContentRepository().getChild("/Settings/Options/Settings/GlobalLink");
  if (!RemoteBeanUtil.isAccessible(gccSettings)) {
    return undefined;
  }

  const gccConfig = gccSettings.getProperties().get("settings") as StructRemoteBean;
  if (!RemoteBeanUtil.isAccessible(gccConfig)) {
    return undefined;
  }

  const dateInFutureInMillieSeconds: number = new Date().getTime() + (MILLISECONDS_FOR_ONE_DAY * as(gccConfig.get("globalLink"), Struct).get("dayOffsetForDueDate"));
  const dateInFuture = new Date(dateInFutureInMillieSeconds);
  return new Calendar({
    year: dateInFuture.getFullYear(),
    month: dateInFuture.getMonth(),
    day: dateInFuture.getDate(),
    offset: -dateInFuture.getTimezoneOffset() * (60 * 1000),
    timeZone: as(session._.getConnection().getContentRepository(), ContentRepositoryImpl).getDefaultTimeZone(),
    normalized: true,
  });
}
