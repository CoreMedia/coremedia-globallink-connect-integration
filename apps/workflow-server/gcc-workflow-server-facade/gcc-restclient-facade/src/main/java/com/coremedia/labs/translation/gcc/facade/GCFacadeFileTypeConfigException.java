package com.coremedia.labs.translation.gcc.facade;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Thrown if the configured file type is not supported by GlobalLink.
 */
@SuppressWarnings("unused")
@NullMarked
public class GCFacadeFileTypeConfigException extends GCFacadeConfigException {
  @Serial
  private static final long serialVersionUID = 2180398367798706948L;

  public GCFacadeFileTypeConfigException() {
  }

  @FormatMethod
  public GCFacadeFileTypeConfigException(@FormatString String message, @Nullable Object... args) {
    super(message, args);
  }

  @FormatMethod
  public GCFacadeFileTypeConfigException(Throwable cause, @FormatString String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeFileTypeConfigException(Throwable cause) {
    super(cause);
  }

  public GCFacadeFileTypeConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
