package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Base failure for any exceptions raised by this facade.
 */
@SuppressWarnings("WeakerAccess")
@DefaultAnnotation(NonNull.class)
public class GCFacadeException extends RuntimeException {
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
