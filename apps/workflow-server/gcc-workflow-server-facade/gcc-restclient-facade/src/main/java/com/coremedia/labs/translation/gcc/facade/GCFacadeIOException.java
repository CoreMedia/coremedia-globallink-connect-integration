package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.io.Serial;

/**
 * Signals some local IO failure.
 */
@SuppressWarnings("unused")
@DefaultAnnotation(NonNull.class)
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
