package com.coremedia.labs.translation.gcc.studio.validation;

import com.coremedia.rest.cap.workflow.validation.impl.translation.DateLiesInFutureValidator;
import com.coremedia.rest.cap.workflow.validation.model.WorkflowValidationParameterModel;
import com.coremedia.rest.validation.Issues;
import com.coremedia.rest.validation.Severity;
import com.coremedia.rest.validation.impl.IssuesImpl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jspecify.annotations.NullMarked;

import java.util.Collections;

@NullMarked
public class GCCDateLiesInFutureValidator extends DateLiesInFutureValidator {

  private final String additionalDateProperty;

  public GCCDateLiesInFutureValidator(String additionalDateProperty) {
    super(additionalDateProperty);
    this.additionalDateProperty = additionalDateProperty;
  }

  @Override
  public void addIssuesIfInvalid(Issues issues, WorkflowValidationParameterModel parameter, Runnable isAbortRequestedRunnable) {
    Object dateObject = parameter.getAdditionalParameters().get(additionalDateProperty);
    LocalizationIssues localizationIssues = new LocalizationIssues(issues);
    if (dateObject == null) {
      localizationIssues.addIssue(Severity.ERROR, null, "dateInvalid_" + additionalDateProperty);
      return;
    }
    super.addIssuesIfInvalid(localizationIssues, parameter, isAbortRequestedRunnable);

  }

  private static class LocalizationIssues extends IssuesImpl<Object> {
     final Issues delegate;

    public LocalizationIssues(Issues delegate) {
      super(null, Collections.emptyList());
      this.delegate = delegate;
    }

    @Override
    public void addIssue(Severity severity, @Nullable String property, String code, Object... arguments) {
      delegate.addIssue(LOCALIZATION_ISSUE_CATEGORY, severity, property, code, arguments);
    }
  }
}
