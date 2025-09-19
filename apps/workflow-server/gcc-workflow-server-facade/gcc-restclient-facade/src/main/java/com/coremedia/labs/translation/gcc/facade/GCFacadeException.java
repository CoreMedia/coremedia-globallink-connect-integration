package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Base failure for any exceptions raised by this facade.
 */
@SuppressWarnings("WeakerAccess")
@NullMarked
public class GCFacadeException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 8255693835569503552L;

  public GCFacadeException() {
    super();
  }

  public GCFacadeException(String message, @Nullable Object... args) {
    super(String.format(message, args));
  }

  public GCFacadeException(Throwable cause, String message, @Nullable Object... args) {
    super(String.format(message, args), cause);
  }

  public GCFacadeException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
