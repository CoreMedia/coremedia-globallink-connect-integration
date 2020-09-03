package com.coremedia.labs.translation.gcc.studio {
import com.coremedia.cap.common.SESSION;
import com.coremedia.cap.content.impl.ContentRepositoryImpl;
import com.coremedia.cms.editor.controlroom.workflow.additionalfields.IAdditionalWorkflowInputFieldMixin;
import com.coremedia.cms.editor.controlroom.workflow.additionalfields.fields.WorkflowDateTimeField;
import com.coremedia.cms.editor.sdk.premular.fields.DateTimePropertyField;
import com.coremedia.cms.editor.sdk.util.ContentLocalizationUtilInternal;
import com.coremedia.collaboration.controlroom.rest.WorkflowSetIssues;
import com.coremedia.ui.bem.SpacingBEMEntities;
import com.coremedia.ui.components.ExtendedTimeField;
import com.coremedia.ui.components.LocalComboBox;
import com.coremedia.ui.components.StatefulDateField;
import com.coremedia.ui.data.Calendar;
import com.coremedia.ui.data.ValueExpression;
import com.coremedia.ui.data.ValueExpressionFactory;
import com.coremedia.ui.data.beanFactory;
import com.coremedia.ui.mixins.ValidationState;
import com.coremedia.ui.plugins.HorizontalSpacingPlugin;

public class GccWorkflowDateTimeField extends DateTimePropertyField implements IAdditionalWorkflowInputFieldMixin {
  private static const DATE_KEY:String = "date";
  private static const TIME_KEY:String = "time";
  private static const TIME_ZONE_KEY:String = "timeZone";

  public function GccWorkflowDateTimeField(config:WorkflowDateTimeField = null) {
    setConfigValues(config);
    super(config);
  }

  /**
   * <p>If you want to set a default value for the WorkflowDateTimeField, you need to define a function that passes {@link Calendar} in the passed calendarCallback, which will
   * be shown as default time. If you can calculate your default calendar synchronously, you can also directly return it in the defaultTimeFunction and ignore the calendarCallback
   * Eg.:
   * <pre>{@code
   * <collab:WorkflowDateTimeField key="test" fieldLabel="test"
                                          defaultTimeFunction="{getCalendar}"/>
   *
   * private function getCalendar(asyncCalendarCallback:Function):Calendar {
      var date:Date = new Date();
      car calendar:Calendar new Calendar({
        year: date.getFullYear(),
        month: date.getMonth(),
        day: date.getDate(),
        hour: 0,
        minute: 0,
        second: 0,
        offset: -date.getTimezoneOffset() * (60 * 1000),
        timeZone: "Europe/Berlin",
        normalized: true
      });
      asyncCalendarCallback(calendar)
   }
   * }
   *
   * </pre>
   *
   * <p>Should your calendar not be normalized, you need to set the property {@link #expectNormalization} to false.
   *
   * <p><b>Note: If you use the defaultTimeFunction, it will overwrite the <i>defaultTimeZoneValueExpression</i></b>
   * </p>
   */
  public var defaultTimeFunction:Function;
  /**
   * set to true if you want to use a remoteValidation for this field.
   * <p>You can use the predefined {@code DateLiesInFutureValidator.java} if you want to check if an entered date lies
   * in the future.
   */
  public var triggerRemoteValidationOnChange:Boolean = false;
  /**
   * Define a function with the signature: <code>function(issues:WorkflowSetIssues, cmp:WorkflowDateTimeField):void</code> that you can use
   * to react to the issues, that are returned by the remote validation, triggered by this field (e.g. use this as a
   * hook to set the state of your component according to the issues returned )
   */
  public var remoteIssuesCallback:Function;

  override protected function onDestroy():void {
    model.removeValueChangeListener(triggerRemoteValidation);
    super.onDestroy();
  }

  public function setValidationState(valid:Boolean):void {
    setValidationStateForDateFields(valid);
  }

  public function getValueForProcess():* {
    var calendar:Calendar = getCalendarFromLocalModel();
    return calendar;
  }

  public function setValueForProcess(value:*):void {
    var calendar:Calendar = value as Calendar;
    if (!calendar) {
      return;
    }
    model.set(DATE_KEY, calendar.getDate());
    model.set(TIME_KEY, calendar.getDate());
    model.set(TIME_ZONE_KEY, (SESSION.getConnection().getContentRepository() as ContentRepositoryImpl).getDefaultTimeZone());
  }

  public function afterWorkflowFieldInitialized():void {
    model.addValueChangeListener(triggerRemoteValidation);
    validValueExpression.addChangeListener(triggerRemoteValidation);
    triggerRemoteValidation();

    if (!readOnlyValueExpression) {
      return;
    }
    forceReadOnlyValueExpression.setValue(readOnlyValueExpression.getValue());
    readOnlyValueExpression.addChangeListener(function (ve:ValueExpression):void {
      forceReadOnlyValueExpression.setValue(ve.getValue());
    });
  }

  public function triggerRemoteValidation():void {
    if (triggerRemoteValidationOnChange) {
      var that:GccWorkflowDateTimeField = this;
      remoteValidationTrigger && remoteValidationTrigger(function (workflowIssue:WorkflowSetIssues):void {
        remoteIssuesCallback(workflowIssue, that);
      });
    }
  }

  private function setValidationStateForDateFields(isValid:Boolean):void {
    var statefulDateField:StatefulDateField = this.queryById("date") as StatefulDateField;
    var extendedTimeField:ExtendedTimeField = this.queryById("time") as ExtendedTimeField;
    var timeZoneField:LocalComboBox = this.queryById("timeZone") as LocalComboBox;

    if (!statefulDateField && !extendedTimeField && !timeZoneField) {
      return;
    }

    if (!isValid) {
      statefulDateField.validationState = ValidationState.ERROR;
      extendedTimeField.validationState = ValidationState.ERROR;
      timeZoneField.validationState = ValidationState.ERROR;
    } else {
      statefulDateField.validationState = undefined;
      extendedTimeField.validationState = undefined;
      timeZoneField.validationState = undefined;
    }
  }

  private function getCalendarFromLocalModel():Calendar {

    var date:Date = model.get(DATE_KEY) as Date;
    var time:Date = model.get(TIME_KEY) as Date;
    var timeZone:String = model.get(TIME_ZONE_KEY);

    if (date && time && timeZone && ContentLocalizationUtilInternal.localizeTimeZoneID(timeZone)) {
      var dataForCalendar:Object = {};

      dataForCalendar.year = date.getFullYear();
      dataForCalendar.month = date.getMonth();
      dataForCalendar.day = date.getDate();
      dataForCalendar.hour = time.getHours();
      dataForCalendar.minute = time.getMinutes();
      dataForCalendar.timeZone = timeZone;

      var calendar:Calendar = new Calendar(dataForCalendar);
      return calendar;
    }
    return null;

  }

  private function getValidValueExpression():ValueExpression {
    if (!validValueExpression) {
      validValueExpression = ValueExpressionFactory.createFromValue(false);
    }
    return validValueExpression;
  }

  private function setConfigValues(config:WorkflowDateTimeField):void {
    config.labelSeparator = config.labelSeparator || "";
    config.labelAlign = config.labelAlign || "top";
    config.bindTo = ValueExpressionFactory.createFromValue(beanFactory.createLocalBean({"properties": {"loaded": true}}));
    config.model = beanFactory.createLocalBean();
    config.isLoadedPropertyName = "loaded";
    this.triggerRemoteValidationOnChange = config.triggerRemoteValidationOnChange;
    this.remoteIssuesCallback = config.remoteIssuesCallback;

    if (config.defaultTimeZoneValueExpression && config.defaultTimeZoneValueExpression.getValue()) {
      config.model.set(TIME_ZONE_KEY, config.defaultTimeZoneValueExpression.getValue())
    }
    if (config.defaultTimeFunction) {
      setDefaultTime(config);
    }

    resetPlugins(config);

    if (!config.forceReadOnlyValueExpression) {
      config.forceReadOnlyValueExpression = ValueExpressionFactory.createFromValue(false);
    }

    if (!config.validValueExpression) {
      config.validValueExpression = getValidValueExpression();
    }
  }

  private function resetPlugins(config:WorkflowDateTimeField):void {
    //the plugins from the DateTimePropertyField need to be overwritten, as they do not really work for the WorkflowDateTimeField
    var spacingPluginConfig:HorizontalSpacingPlugin = HorizontalSpacingPlugin({});
    spacingPluginConfig.modifier = SpacingBEMEntities.HORIZONTAL_SPACING_MODIFIER_200;
    var spacingPlugin:HorizontalSpacingPlugin = new HorizontalSpacingPlugin(spacingPluginConfig);
    config.plugins ? config.plugins.push(spacingPlugin) : config.plugins = [spacingPlugin];
  }

  /**
   * This function sets a {@link Calendar} as default time. This can either happen synchronously or asynchronously.
   * Therefore we process the return value, as well as the callback
   */
  private function setDefaultTime(config:WorkflowDateTimeField):void {
    var calendarValueExpression:ValueExpression = config.bindTo.extendBy('properties', config.propertyName);
    var calendar:Calendar = config.defaultTimeFunction(function (calendar:Calendar):void {
      if (calendar is Calendar) {
        config.model.set(TIME_ZONE_KEY, calendar.getTimeZone());
        config.model.set(TIME_KEY, calendar.getDate());
        config.model.set(DATE_KEY, calendar.getDate());
      }
    }) as Calendar;
    if (calendar) {
      calendarValueExpression.setValue(calendar);
    }
  }

  /** @inheritDoc */
  public native function get key():String;

  /** @inheritDoc */
  public native function set key(key:String):void;

  /** @inheritDoc */
  public native function get taskVE():ValueExpression;

  /** @inheritDoc */
  public native function set taskVE(taskVE:ValueExpression):void;

  /** @inheritDoc */
  public native function get remoteValidationTrigger():Function;

  /** @inheritDoc */
  public native function set remoteValidationTrigger(remoteValidationTrigger:Function):void;

  /** @inheritDoc */
  public native function get localValidationTrigger():Function;

  /** @inheritDoc */
  public native function set localValidationTrigger(localValidationTrigger:Function):void;

  /** @inheritDoc */
  public native function get writeValueToProcessTrigger():Function;

  /** @inheritDoc */
  public native function set writeValueToProcessTrigger(writeValueTrigger:Function):void;

  /** @inheritDoc */
  public native function get readOnlyValueExpression():ValueExpression;

  /** @inheritDoc */
  public native function set readOnlyValueExpression(readOnlyValueExpression:ValueExpression):void;

  /** @inheritDoc */
  public native function get builtIn():Boolean;

  /** @private */
  public native function set builtIn(builtIn:Boolean):void;

}
}
