package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.CapException;
import com.coremedia.cap.common.RepositoryNotAvailableException;
import com.coremedia.cap.errorcodes.CapErrorCodes;
import com.google.common.collect.ImmutableSet;
import org.omg.CORBA.SystemException;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Predicate that returns {@code true} for {@link Throwable}s caused by unavailable Unified API repositories
 * or CORBA {@link SystemException communication exceptions}.
 */
public class RepositoryUnavailableThrowablePredicate implements Predicate<Throwable> {

  public static final RepositoryUnavailableThrowablePredicate INSTANCE = new RepositoryUnavailableThrowablePredicate();

  private static final Set<String> REPOSITORY_UNAVAILABLE_ERROR_CODES = ImmutableSet.of(
          CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE,
          CapErrorCodes.USER_REPOSITORY_UNAVAILABLE,
          CapErrorCodes.WORKFLOW_REPOSITORY_UNAVAILABLE,
          CapErrorCodes.CAPLIST_REPOSITORY_UNAVAILABLE,
          CapErrorCodes.REPOSITORY_NOT_AVAILABLE
  );

  private RepositoryUnavailableThrowablePredicate() {
  }

  public static boolean matches(Throwable throwable) {
    return INSTANCE.test(throwable);
  }

  @Override
  public boolean test(Throwable throwable) {
    Throwable cause = throwable;
    while (cause != null) {
      if (isRepositoryUnavailableException(cause)) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private static boolean isRepositoryUnavailableException(Throwable exception) {
    return exception instanceof RepositoryNotAvailableException
           || exception instanceof SystemException
           || (exception instanceof CapException && REPOSITORY_UNAVAILABLE_ERROR_CODES.contains(((CapException) exception).getErrorCode()));
  }
}
