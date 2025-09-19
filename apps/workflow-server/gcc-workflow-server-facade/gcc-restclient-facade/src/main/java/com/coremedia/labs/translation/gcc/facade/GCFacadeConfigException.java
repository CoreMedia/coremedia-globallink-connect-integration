package com.coremedia.labs.translation.gcc.facade;

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

  public GCFacadeConfigException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public GCFacadeConfigException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeConfigException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
