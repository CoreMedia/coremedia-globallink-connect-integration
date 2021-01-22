package com.coremedia.labs.translation.gcc.studio {
import com.coremedia.cap.common.SESSION;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.impl.ContentRepositoryImpl;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.cap.workflow.WorkflowObject;
import com.coremedia.cap.workflow.WorkflowObjectProperties;
import com.coremedia.cms.editor.configuration.StudioPlugin;
import com.coremedia.cms.editor.sdk.EditorContextImpl;
import com.coremedia.cms.editor.sdk.editorContext;
import com.coremedia.cms.studio.base.cap.models.locale.ILocalesService;
import com.coremedia.ui.components.IconButton;
import com.coremedia.ui.data.Blob;
import com.coremedia.ui.data.Calendar;
import com.coremedia.ui.data.Locale;
import com.coremedia.ui.data.ValueExpression;
import com.coremedia.ui.data.ValueExpressionFactory;
import com.coremedia.ui.data.impl.RemoteService;
import com.coremedia.ui.messagebox.MessageBoxUtil;

import ext.data.JsonStore;
import ext.data.Store;
import ext.grid.GridPanel;
import ext.panel.Panel;

import mx.resources.ResourceManager;

public class GccStudioPluginBase extends StudioPlugin {

  private static const DATE_KEY:String = "date";
  private static const TIME_KEY:String = "time";
  private static const TIME_ZONE_KEY:String = "timeZone";
  private static const MILLISECONDS_FOR_ONE_DAY:Number = 86400000;

  private static const TRANSLATION_GLOBAL_LINK_PROCESS_NAME:String = "TranslationGlobalLink";

  private static const CANCEL_REQUESTED_VARIABLE_NAME:String = "cancelRequested";
  private static const SUBJECT_VARIABLE_NAME:String = "subject";
  private static const CANCELLATION_ALLOWED_VARIABLE_NAME:String = "cancellationAllowed";
  private static const BLOB_FILE_PROCESS_VARIABLE_NAME:String = "translationResultXliff";
  private static const GLOBAL_LINK_SUBMISSION_STATUS_VARIABLE_NAME:String = "globalLinkSubmissionStatus";
  private static const TRANSLATION_RESULT_XLIFF_VARIABLE_NAME:String = "translationResultXliff";

  private static const ERROR_TASK_NAME:String = "HandleDownloadTranslationError";
  private static const HANDLE_SEND_TRANSLATION_REQUEST_ERROR_TASK_NAME:String = "HandleSendTranslationRequestError";
  private static const HANDLE_DOWNLOAD_TRANSLATION_ERROR_TASK_NAME:String = "HandleDownloadTranslationError";
  private static const HANDLE_CANCEL_TRANSLATION_ERROR_TASK_NAME:String = "HandleCancelTranslationError";

  private static const CANCELLATION_CONFIRMED_SUBMISSION_STATE:String = "CANCELLATION_CONFIRMED";
  private static const CANCELLED_SUBMISSION_STATE:String = "CANCELLED";
  private static const UNAVAILABLE_SUBMISSION_STATE:String = 'unavailable';

  internal var dayOffsetValueExpression:ValueExpression;

  private var lastAddedButton:IconButton = null;

  private var completedLocalesQuickTipTextValueExpression:ValueExpression;

  public function GccStudioPluginBase(config:GccStudioPluginBase = null) {
    super(config);
  }


  internal static function transformSubmissionStatus(value:String):String {
    if (!value) {
      value = UNAVAILABLE_SUBMISSION_STATE;
    }
    var status:String = ResourceManager.getInstance().getString('com.coremedia.labs.translation.gcc.studio.GccProcessDefinitions', 'TranslationGlobalLink_submission_status_' + value);
    if (!status) {
      status = value;
    }
    return status;
  }

  internal static function transformSubmissionId(value:String):String {
    if (!value) {
      value = UNAVAILABLE_SUBMISSION_STATE;
    }
    return value;
  }

  internal static function getCustomProcessIconFunction():Function {
    return function (data:*):String {
      //show warning icon for error tasks
      if (data
              && (data.taskDefinitionName === HANDLE_SEND_TRANSLATION_REQUEST_ERROR_TASK_NAME || data.taskDefinitionName === HANDLE_DOWNLOAD_TRANSLATION_ERROR_TASK_NAME || data.taskDefinitionName === HANDLE_CANCEL_TRANSLATION_ERROR_TASK_NAME)
              && !data.withWarning && !data.accepted && !data.escalated) {
        return resourceManager.getString('com.coremedia.icons.CoreIcons', 'warning');
      }

      // show cancelled icon for cancelled submission
      if (data && data.bean as WorkflowObject) {
        var workflowObject:WorkflowObject = data.bean as WorkflowObject;

        var process:Process = getProcess(workflowObject);

        if (processAvailable(process) && process.isLoaded()) {
          var gccSubmissionsState:String = process.getProperties().get(GLOBAL_LINK_SUBMISSION_STATUS_VARIABLE_NAME) as String;
          var cancelRequested:Boolean = process.getProperties().get(CANCEL_REQUESTED_VARIABLE_NAME) as Boolean || gccSubmissionsState === CANCELLATION_CONFIRMED_SUBMISSION_STATE || gccSubmissionsState === CANCELLED_SUBMISSION_STATE;
          if (cancelRequested) {
            return resourceManager.getString('com.coremedia.icons.CoreIcons', 'abort_workflow');
          }
        }
      }
    };
  }

  internal static function getTranslationCancelToolBarButtonGCC(workflowObjectVE:ValueExpression, panel:Panel):IconButton {

    var process:Process = getProcess(workflowObjectVE.getValue());

    var buttonCfg:IconButton = IconButton({});
    buttonCfg.itemId = "cancelButtonID";
    buttonCfg.tooltip = ResourceManager.getInstance().getString('com.coremedia.labs.translation.gcc.studio.GccProcessDefinitions', 'Action_Tooltip_Cancel_Process');
    buttonCfg.iconCls = ResourceManager.getInstance().getString('com.coremedia.icons.CoreIcons', 'remove');
    buttonCfg.handler = function ():void {
      var processName:String = process.getProperties().get(SUBJECT_VARIABLE_NAME) as String;
      var messageText:String = ResourceManager.getInstance().getString('com.coremedia.labs.translation.gcc.studio.GccProcessDefinitions', 'confirm_cancellation', [processName]);

      MessageBoxUtil.showPrompt("Cancel Workflow", messageText, function (result:String):void {
                if (process && result === "ok") {
                  process.getProperties().set(CANCEL_REQUESTED_VARIABLE_NAME, true);
                  workflowObjectVE.setValue(undefined);
                  panel["view"] && panel["view"].refresh();
                }
              });
    };

    if (mayButtonBeAdded(process, panel)) {
      return new IconButton(buttonCfg);
    }
  }


  private function getDayOffsetFromSettings(offSetValueExpression:ValueExpression):void {
    var globalLinkSettings:Content = SESSION.getConnection().getContentRepository().getChild("/Settings/Options/Settings/GlobalLink");
    if (globalLinkSettings) {
      globalLinkSettings.load(function ():void {
        var settings:Struct = globalLinkSettings.getProperties().get("settings") as Struct;
        if (settings) {
          var gccSettingsVE:ValueExpression = ValueExpressionFactory.createFromFunction(function ():Struct {
            var gccSettings:Struct = settings.get("globalLink") as Struct;
            if (!gccSettings) {
              return undefined;
            }
            return gccSettings;
          });

          gccSettingsVE.loadValue(function ():void {
            offSetValueExpression.setValue(gccSettingsVE.getValue().get("dayOffsetForDueDate") as int);
          });
        }
      });
    }
  }


  //noinspection JSMethodCanBeStatic => method cannot be called if static
  internal function downloadXliff(task:Task):Function {
    var currentDate:Date = new Date();
    var uri:String = RemoteService.calculateRequestURI("downloadBlob/") +
            "?process=" + task.getContainingProcess().getId() +
            "&blobProcessVariable=" + BLOB_FILE_PROCESS_VARIABLE_NAME +
            "&" + currentDate.getTime();
    window.open(uri);
  }

  /**
   * Method that validates the given process, if a button can be added to the panel header
   * @param process to validate
   * @param panel to validate if the button can be added to
   * @return true if the button may be added to the panel header
   */
  private static function mayButtonBeAdded(process:Process, panel:Panel):Boolean {
    var cancellationAllowed:Boolean = processAvailable(process) && process.getProperties().get(CANCELLATION_ALLOWED_VARIABLE_NAME) as Boolean;

    //we need to check if the store is not empty, to make sure not to show the button for an empty panel
    var jsonStore:JsonStore = panel["store"] as JsonStore;
    var panelJsonStoreNotEmpty:Boolean = jsonStore && jsonStore.data && jsonStore.data.length > 0;

    var isTranslationGlobalLink:Boolean = processAvailable(process) && process.getDefinition().getName() === TRANSLATION_GLOBAL_LINK_PROCESS_NAME;

    var cancelAlreadyRequested:Boolean = processAvailable(process) && process.getProperties().get(CANCEL_REQUESTED_VARIABLE_NAME) as Boolean;

    return isTranslationGlobalLink && panelJsonStoreNotEmpty && cancellationAllowed && !cancelAlreadyRequested;
  }

  internal function defaultTimeFunction(asyncCalendarCallback:Function):Calendar {
    if (!asyncCalendarCallback) {
      return undefined;
    }

    dayOffsetValueExpression = ValueExpressionFactory.createFromValue(undefined);

    //Settings must be loaded, therefore calculation the default calendar is asynch
    dayOffsetValueExpression.addChangeListener(function (offsetVE:ValueExpression):void {
      var dateInFutureInMillieSeconds:Number = new Date().getTime() + (MILLISECONDS_FOR_ONE_DAY * offsetVE.getValue());
      var dateInFuture:Date = new Date(dateInFutureInMillieSeconds);
      var defaultCalendar:Calendar = new Calendar({
        year: dateInFuture.getFullYear(),
        month: dateInFuture.getMonth(),
        day: dateInFuture.getDate(),
        offset: -dateInFuture.getTimezoneOffset() * (60 * 1000),
        timeZone: (SESSION.getConnection().getContentRepository() as ContentRepositoryImpl).getDefaultTimeZone(),
        normalized: true
      });
      asyncCalendarCallback(defaultCalendar);
    });
    getDayOffsetFromSettings(dayOffsetValueExpression);
  }

  //noinspection JSMethodCanBeStatic => method cannot be called if static
  internal function downloadAvailable(taskVE:ValueExpression):Boolean {
    if (taskVE && taskVE.getValue() && taskVE.getValue().getDefinition().getName() === ERROR_TASK_NAME) {
      var properties:WorkflowObjectProperties = taskVE.getValue().getContainingProcess() && taskVE.getValue().getContainingProcess().getProperties();
      var downloadAvailable:Blob = properties && properties.get(TRANSLATION_RESULT_XLIFF_VARIABLE_NAME) as Blob;
      return !!downloadAvailable;
    }
    return false;
  }

  internal function getButtons():Function {
    return function (panel:GridPanel, processCategory:String, panelType:String, selectedWorkflowObjectVE:ValueExpression):Array {

      var buttonFunction:Function = function ():void {
        lastAddedButton && panel.getTopToolbar().remove(lastAddedButton);
        var translationCancelToolBarButtonGCC:IconButton = getTranslationCancelToolBarButtonGCC(selectedWorkflowObjectVE, panel);
        if (translationCancelToolBarButtonGCC) {
          panel.getTopToolbar().add(translationCancelToolBarButtonGCC);
          lastAddedButton = translationCancelToolBarButtonGCC;
        }
      };

      if (panelType === "processListPanel" && selectedWorkflowObjectVE) {
        selectedWorkflowObjectVE.addChangeListener(buttonFunction);
        var cancellationAllowedVE:ValueExpression = ValueExpressionFactory.createFromFunction(function ():Boolean {
          var process:Process = getProcess(selectedWorkflowObjectVE.getValue());
          if (process) {
            return process.getProperties().get(CANCELLATION_ALLOWED_VARIABLE_NAME);
          }
        });

        cancellationAllowedVE.addChangeListener(buttonFunction);

        //we want to recalculate the buttons when something was removed from the store
        var jsonStore:JsonStore = panel["store"] as JsonStore;
        jsonStore && jsonStore.on("remove", function (store:Store, records:Array):void {
          var removed:WorkflowObject = records[0].data.bean as WorkflowObject;
          if (selectedWorkflowObjectVE.getValue() === removed) {
            selectedWorkflowObjectVE.setValue(undefined);
          }
          buttonFunction();
          panel.getView() && panel.getView().refresh();
        });
      }

    }
  }

  private static function processAvailable(process:Process):Boolean {
    return process && process.getState().exists;
  }

  private static function getProcess(workflowObject:WorkflowObject):Process {
    if (workflowObject && workflowObject.isTask()) {
      var task:Task = workflowObject as Task;
      return task.getContainingProcess();
    } else if (workflowObject && workflowObject.isProcess()) {
      return workflowObject as Process;
    }
  }


  /**
   * Method will take an Array of Locales, and return a localized version of the Locale, should there be just one within the Array.
   * In case there are more Locales in the given Array, the method will return a text that states how many Locales
   * are within the Array. Also a quickTipText for the given Locales will be computed and stored within the quickTipValueExpression.
   * @param locales to compute the text and quickTipText from
   * @return a localized String value of the given Array (should there be just one) or a text, stating how many Locales exist.
   */
  protected function convertLocales(locales:Array):String {
    if (locales) {
      var localesService:ILocalesService = (editorContext as EditorContextImpl).getLocalesService();
      createQuickTipText(locales, localesService, getCompletedLocalesQuickTipTextValueExpression());

      if (locales.length === 1) {
        var locale:Locale = localesService.getLocale(locales[0]);
        var displayNameOfLocale:String = locale && locale.getDisplayName();
        return displayNameOfLocale ? displayNameOfLocale : "";
      }
      return ResourceManager.getInstance().getString('com.coremedia.labs.translation.gcc.studio.GccProcessDefinitions', 'TranslationGlobalLink_Multi_Target_Locale_Text', [locales.length]);
    }
    return "";
  }


  /**
   * Method will compute a text from a given Array of Locales, localize the Locales via the given localesService
   * and store the text in the given quickTipValueExpression.
   * @param locales to compute a text from
   * @param localesService to localize the locales
   * @param quickTipValueExpression to store the computed text into
   */
  private function createQuickTipText(locales:Array, localesService:ILocalesService, quickTipValueExpression:ValueExpression):void {
    var localeQuickTipText:String = "";
    locales.forEach(function (localeString:String):void {
      var locale:Locale = localesService.getLocale(localeString);
      if (localeQuickTipText == "") {
        localeQuickTipText = locale && locale.getDisplayName();
      } else {
        localeQuickTipText = localeQuickTipText + ", " + (locale && locale.getDisplayName());
      }
    });
    quickTipValueExpression.setValue(localeQuickTipText);
  }

  internal function getCompletedLocalesQuickTipTextValueExpression():ValueExpression {
    if (!completedLocalesQuickTipTextValueExpression) {
      completedLocalesQuickTipTextValueExpression = ValueExpressionFactory.createFromValue("");
    }
    return completedLocalesQuickTipTextValueExpression;
  }

}
}
