package com.coremedia.labs.translation.gcc.facade;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Signals an error with the GCC REST Backend via GCC Java RestClient because of an invalid or
 * expired API key.
 */
@SuppressWarnings("unused")
@NullMarked
public class GCFacadeAccessException extends GCFacadeException {
  @Serial
  private static final long serialVersionUID = -4226793602127027111L;

  public GCFacadeAccessException() {
  }

  @FormatMethod
  public GCFacadeAccessException(@FormatString String message, @Nullable Object... args) {
    super(message, args);
  }

  @FormatMethod
  public GCFacadeAccessException(Throwable cause, @FormatString String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public GCFacadeAccessException(Throwable cause) {
    super(cause);
  }

  protected GCFacadeAccessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
