package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.CapException;

import java.io.Serial;

import static java.util.Arrays.stream;

class GlobalLinkWorkflowException extends CapException {
  @Serial
  private static final long serialVersionUID = -6281116907297375413L;

  GlobalLinkWorkflowException(String errorCode, String message, Object... parameters) {
    this(errorCode, message, null, parameters);
  }

  GlobalLinkWorkflowException(String errorCode, String message, Throwable cause, Object... parameters) {
    super(
      "globallink",
      errorCode,
      errorCode,
      message,
      stream(parameters).map(o -> o == null ? null : String.valueOf(o)).toArray(String[]::new),
      cause);
  }

}
