package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;

/**
 * A scenario defines a certain behavior of the mock GCC facade.
 * It may implement one or more of the interceptor interfaces
 * {@link TranslationInterceptor}, {@link SubmissionInterceptor},
 * and {@link DownloadInterceptor} to modify the behavior of the mock GCC
 * facade.
 * <p>
 * Typically, an implementation just needs to specify an ID via
 * {@link #id()} and implement one or more of the interceptor interfaces.
 * The other interceptor methods will return no-op implementations by default.
 * <p>
 * <strong>Note:</strong>
 * Do not forget adding your new scenario to the
 * {@code META-INF/services/com.coremedia.labs.translation.gcc.facade.mock.scenarios.Scenario}
 * file to make it available via Java's ServiceLoader mechanism.
 * <p>
 * You can activate a scenario by setting the {@code mock.scenario} setting
 * to the scenario ID (case-insensitive).
 *
 * @since 2506.0.1-1
 */
@NullMarked
public interface Scenario {
  /**
   * The ID to reference in settings in {@code mock.scenario} setting.
   * Matching is case-insensitive.
   *
   * @return scenario ID
   */
  String id();

  /**
   * Return the translation interceptor for this scenario.
   *
   * @return the translation interceptor for this scenario
   * @implSpec If this scenario implements {@link TranslationInterceptor},
   * return this, otherwise return a no-op implementation.
   */
  default TranslationInterceptor translate() {
    if (this instanceof TranslationInterceptor me) {
      return me;
    }
    return TranslationInterceptor.NO_OPERATION;
  }

  /**
   * Return the submission interceptor for this scenario.
   *
   * @return the submission interceptor for this scenario
   * @implSpec If this scenario implements {@link SubmissionInterceptor},
   * return this, otherwise return a no-op implementation.
   */
  default SubmissionInterceptor submission() {
    if (this instanceof SubmissionInterceptor me) {
      return me;
    }
    return SubmissionInterceptor.NO_OPERATION;
  }

  /**
   * Return the download interceptor for this scenario.
   *
   * @return the download interceptor for this scenario
   * @implSpec If this scenario implements {@link DownloadInterceptor},
   * return this, otherwise return a no-op implementation.
   */
  default DownloadInterceptor download() {
    if (this instanceof DownloadInterceptor me) {
      return me;
    }
    return DownloadInterceptor.NO_OPERATION;
  }

  /**
   * Return the cancelation interceptor for this scenario.
   *
   * @return the cancelation interceptor for this scenario
   * @implSpec If this scenario implements {@link CancelationInterceptor},
   * return this, otherwise return a no-op implementation.
   */
  default CancelationInterceptor cancelation() {
    if (this instanceof CancelationInterceptor me) {
      return me;
    }
    return CancelationInterceptor.NO_OPERATION;
  }

  /**
   * Return the upload interceptor for this scenario.
   *
   * @return the upload interceptor for this scenario
   * @implSpec If this scenario implements {@link UploadInterceptor},
   * return this, otherwise return a no-op implementation.
   */
  default UploadInterceptor upload() {
    if (this instanceof UploadInterceptor me) {
      return me;
    }
    return UploadInterceptor.NO_OPERATION;
  }
}
