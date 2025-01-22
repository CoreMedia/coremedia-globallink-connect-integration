package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Properties to be used in {@code GlobalLink} settings document.
 */
@SuppressWarnings("UtilityClassCanBeEnum")
@DefaultAnnotation(NonNull.class)
public final class GCConfigProperty {
  /**
   * Root node for GCC Settings in Struct.
   * <p>
   * <strong>Type</strong>: {@code Struct}
   */
  public static final String KEY_GLOBALLINK_ROOT = "globalLink";

  /**
   * The API URL for GCC REST endpoint (v3).
   * <p>
   * <strong>Type</strong>: {@code String}
   */
  public static final String KEY_URL = "url";

  /**
   * Connector Key for GCC connection.
   * <p>
   * <strong>Type</strong>: {@code String}
   */
  public static final String KEY_KEY = "key";

  /**
   * API Key for GCC connection.
   * <p>
   * <strong>Type</strong>: {@code String}
   */
  public static final String KEY_API_KEY = "apiKey";

  /**
   * GlobalLink file type to use. Optional setting.
   * <p>
   * Defaults to first file type from list of file types that GlobalLink returns
   * in its connector config.
   * <p>
   * <strong>Type</strong>: {@code String}
   */
  public static final String KEY_FILE_TYPE = "fileType";

  /**
   * Type of facade to instantiate. Optional key. Will default to
   * {@link #VALUE_TYPE_DEFAULT}.
   * <p>
   * <strong>Type</strong>: {@code String}
   */
  public static final String KEY_TYPE = "type";

  /**
   * Determines if the name of the submitter is passed to GlobalLink. For
   * privacy reason this should be deactivated by default.
   * <p>
   * <strong>Type</strong>: {@code Boolean}
   */
  public static final String KEY_IS_SEND_SUBMITTER = "isSendSubmitter";

  /**
   * Behavioral configuration for submission names.
   * <p>
   * <strong>Type</strong>: {@code Struct}
   *
   * @see com.coremedia.labs.translation.gcc.facade.config.GCSubmissionName
   */
  public static final String KEY_SUBMISSION_NAME = "submissionName";

  /**
   * Behavioral configuration for submission instructions.
   * <p>
   * <strong>Type</strong>: {@code Struct}
   *
   * @see com.coremedia.labs.translation.gcc.facade.config.GCSubmissionInstruction
   */
  public static final String KEY_SUBMISSION_INSTRUCTION = "submissionInstruction";

  /**
   * Default type for facades. Will be used also if unset or if the type
   * cannot be parsed/is unknown.
   */
  public static final String VALUE_TYPE_DEFAULT = "default";

  private GCConfigProperty() {
  }
}
