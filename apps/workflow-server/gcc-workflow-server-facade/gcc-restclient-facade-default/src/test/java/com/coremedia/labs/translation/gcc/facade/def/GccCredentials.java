package com.coremedia.labs.translation.gcc.facade.def;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(GccCredentialsExtension.class)
@Inherited
@Documented
@NullMarked
public @interface GccCredentials {
  /**
   * An optional profile to <em>prefer</em> over the default credentials.
   * Usable, for example, to override some configuration parameters. If
   * given, data from a file named
   * {@code .gcc.<profile>.properties} is preferred over settings from default
   * {@code .gcc.properties}.
   *
   * @return profile to prefer
   */
  String value() default "";
}
