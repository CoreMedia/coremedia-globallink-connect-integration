package com.coremedia.labs.translation.gcc.facade;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Thrown if the configured connection key is unavailable.
 */
@SuppressWarnings("unused")
@NullMarked
public class GCFacadeConnectorKeyConfigException extends GCFacadeConfigException {
  @Serial
  private static final long serialVersionUID = -4124879618192150569L;

  public GCFacadeConnectorKeyConfigException() {
  }

  @FormatMethod
  public GCFacadeConnectorKeyConfigException(@FormatString String message, @Nullable Object... args) {
    super(message, args);
  }

  @FormatMethod
  public GCFacadeConnectorKeyConfigException(Throwable cause, @FormatString String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeConnectorKeyConfigException(Throwable cause) {
    super(cause);
  }

  public GCFacadeConnectorKeyConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
