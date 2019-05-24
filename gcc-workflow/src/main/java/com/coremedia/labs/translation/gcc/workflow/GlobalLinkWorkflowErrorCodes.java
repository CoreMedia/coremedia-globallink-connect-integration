package com.coremedia.labs.translation.gcc.workflow;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotation(NonNull.class)
class GlobalLinkWorkflowErrorCodes {

  // ==== 10###: General/Unknown Problems
  static final String UNKNOWN_ERROR   = "GCC-WF-10000";

  // ==== 20###: GCC RestClient Problems
  static final String GLOBAL_LINK_COMMUNICATION_ERROR = "GCC-WF-20000";

  // ==== 30###: Local IO Problems
  static final String LOCAL_IO_ERROR = "GCC-WF-30001";

  // ==== 40###: Configuration Problems
  static final String SETTINGS_ERROR                    = "GCC-WF-40000";
  static final String SETTINGS_FILE_TYPE_ERROR          = "GCC-WF-40001";
  static final String ILLEGAL_SUBMISSION_ID_ERROR       = "GCC-WF-40050";

  // ==== 50###: XLIFF Problems
  static final String XLIFF_EXPORT_FAILURE = "GCC-WF-50050";

  // ==== 60###: GCC State Problems
  static final String SUBMISSION_CANCEL_FAILURE = "GCC-WF-61001";

  private GlobalLinkWorkflowErrorCodes() {
  }

}
