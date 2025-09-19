package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Signals some local IO failure.
 */
@SuppressWarnings("unused")
@NullMarked
public class GCFacadeIOException extends GCFacadeException {
  @Serial
  private static final long serialVersionUID = 8022138209281797363L;

  public GCFacadeIOException() {
  }

  public GCFacadeIOException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public GCFacadeIOException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeIOException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
