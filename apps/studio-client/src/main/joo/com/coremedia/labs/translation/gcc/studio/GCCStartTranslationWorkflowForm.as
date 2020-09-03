package com.coremedia.labs.translation.gcc.studio {
import com.coremedia.cms.editor.controlroom.workflow.translation.DefaultStartTranslationWorkflowForm;

public class GCCStartTranslationWorkflowForm extends DefaultStartTranslationWorkflowForm{


  public function GCCStartTranslationWorkflowForm(config:DefaultStartTranslationWorkflowForm = null) {
    super(config);
  }

  override public function isPullTranslation():Boolean {
    //for this workflow a PullTranslation should be disabled
    return false;
  }
}
}
