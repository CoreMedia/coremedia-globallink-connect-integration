package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.CapException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@NullMarked
class GlobalLinkWorkflowException extends CapException {
  @Serial
  private static final long serialVersionUID = -6281116907297375413L;

  GlobalLinkWorkflowException(String errorCode, String message, Object... parameters) {
    this(errorCode, message, null, parameters);
  }

  GlobalLinkWorkflowException(String errorCode, String message, @Nullable Throwable cause, @Nullable Object... parameters) {
    super(
      "globallink",
      errorCode,
      errorCode,
      message,
      asNullableStringParameters(parameters),
      cause);
  }

  private static @Nullable String[] asNullableStringParameters(@Nullable Object[] parameters) {
    List<@Nullable String> result = new ArrayList<>();
    for (Object parameter : parameters) {
      if (parameter == null) {
        result.add(null);
      } else {
        result.add(parameter.toString());
      }
    }
    return result.toArray(new @Nullable String[0]);
  }
}
