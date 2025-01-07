package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.Serial;

/**
 * Signals an error with the GCC REST Backend via GCC Java RestClient because of an invalid or
 * expired API key.
 */
@SuppressWarnings("unused")
@DefaultAnnotation(NonNull.class)
public class GCFacadeAccessException extends GCFacadeException {
  @Serial
  private static final long serialVersionUID = -4226793602127027111L;

  public GCFacadeAccessException() {
  }

  public GCFacadeAccessException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public GCFacadeAccessException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeAccessException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeAccessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
