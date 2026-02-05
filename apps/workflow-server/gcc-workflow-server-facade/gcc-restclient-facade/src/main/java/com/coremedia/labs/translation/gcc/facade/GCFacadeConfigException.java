package com.coremedia.labs.translation.gcc.facade;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Signals a configuration problem, like especially invalid settings.
 */
@SuppressWarnings("unused")
@NullMarked
public class GCFacadeConfigException extends GCFacadeException {
  @Serial
  private static final long serialVersionUID = 6482445402768874493L;

  public GCFacadeConfigException() {
  }

  @FormatMethod
  public GCFacadeConfigException(@FormatString String message, @Nullable Object... args) {
    super(message, args);
  }

  @FormatMethod
  public GCFacadeConfigException(Throwable cause, @FormatString String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeConfigException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
