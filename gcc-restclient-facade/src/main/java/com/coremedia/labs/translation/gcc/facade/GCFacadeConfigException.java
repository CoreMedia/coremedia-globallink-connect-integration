package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Signals a configuration problem, like especially invalid settings.
 */
@SuppressWarnings("unused")
@DefaultAnnotation(NonNull.class)
public class GCFacadeConfigException extends GCFacadeException {
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
