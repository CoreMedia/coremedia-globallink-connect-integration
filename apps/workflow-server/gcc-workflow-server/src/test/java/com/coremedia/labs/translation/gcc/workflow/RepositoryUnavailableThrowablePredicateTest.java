package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cache.EvaluationException;
import com.coremedia.cap.common.CapException;
import com.coremedia.cap.common.RepositoryNotAvailableException;
import com.coremedia.cap.errorcodes.CapErrorCodes;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.springframework.beans.InvalidPropertyException;

import java.lang.reflect.InvocationTargetException;

import static com.coremedia.labs.translation.gcc.workflow.RepositoryUnavailableThrowablePredicate.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryUnavailableThrowablePredicateTest {

  @Test
  void testInclude() {
    RepositoryNotAvailableException repositoryNotAvailableException = new RepositoryNotAvailableException("foo", null, null);
    CapException contentRepositoryUnvailableCapException = new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null);

    assertTrue(INSTANCE.test(repositoryNotAvailableException));
    assertTrue(INSTANCE.test(contentRepositoryUnvailableCapException));
    assertTrue(INSTANCE.test(new CapException("foo", CapErrorCodes.WORKFLOW_REPOSITORY_UNAVAILABLE, null, null)));
    assertTrue(INSTANCE.test(new CapException("foo", CapErrorCodes.USER_REPOSITORY_UNAVAILABLE, null, null)));
    assertTrue(INSTANCE.test(new CapException("foo", CapErrorCodes.CAPLIST_REPOSITORY_UNAVAILABLE, null, null)));
    assertTrue(INSTANCE.test(new CapException("foo", CapErrorCodes.REPOSITORY_NOT_AVAILABLE, null, null)));

    assertTrue(INSTANCE.test(new CapException("foo", null, null, repositoryNotAvailableException)));
    assertTrue(INSTANCE.test(new InvalidPropertyException(Object.class, "foo", "bar", repositoryNotAvailableException)));
    assertTrue(INSTANCE.test(new InvocationTargetException(repositoryNotAvailableException)));
    assertTrue(INSTANCE.test(new EvaluationException(repositoryNotAvailableException)));
    assertTrue(INSTANCE.test(new RuntimeException(repositoryNotAvailableException)));
    assertTrue(INSTANCE.test(new RuntimeException(new RuntimeException(repositoryNotAvailableException))));

    assertTrue(INSTANCE.test(new RuntimeException(new RuntimeException(contentRepositoryUnvailableCapException))));

    // observed during debugging when the content server was restarted while a corba call was made:
    // org.omg.CORBA.OBJECT_NOT_EXIST: FINE: 02510002: The server ID in the target object key does not match the server key expected by the server  vmcid: OMG  minor code: 2  completed: No
    assertTrue(INSTANCE.test(new CapException("content", CapErrorCodes.UNEXPECTED_RUNTIME_EXCEPTION, null, new OBJECT_NOT_EXIST())));
  }

  @Test
  void testExclude() {
    assertFalse(INSTANCE.test(new RuntimeException()));
    assertFalse(INSTANCE.test(new RuntimeException(new RuntimeException())));
    assertFalse(INSTANCE.test(new InvocationTargetException(new RuntimeException())));
    assertFalse(INSTANCE.test(new CapException("foo", null, null, null)));
    assertFalse(INSTANCE.test(new CapException("foo", CapErrorCodes.CANNOT_READ_BLOB, null, null)));
  }
}
