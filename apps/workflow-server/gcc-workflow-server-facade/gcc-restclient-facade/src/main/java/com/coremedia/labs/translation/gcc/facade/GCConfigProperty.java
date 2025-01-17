package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Properties to be used in {@code GlobalLink} settings document.
 */
@DefaultAnnotation(NonNull.class)
public final class GCConfigProperty {
  /**
   * Root node for GCC Settings in Struct.
   */
  public static final String KEY_GLOBALLINK_ROOT = "globalLink";

  /**
   * The API URL for GCC REST endpoint (v3).
   */
  public static final String KEY_URL = "url";

  /**
   * Connector Key for GCC connection.
   */
  public static final String KEY_KEY = "key";

  /**
   * API Key for GCC connection.
   */
  public static final String KEY_API_KEY = "apiKey";

  /**
   * GlobalLink file type to use. Optional setting.
   * Defaults to first file type from list of file types that GlobalLink returns in its connector config.
   */
  public static final String KEY_FILE_TYPE = "fileType";

  /**
   * Type of facade to instantiate. Optional key. Will default to
   * {@link #VALUE_TYPE_DEFAULT}.
   */
  public static final String KEY_TYPE = "type";

  /**
   * Boolean that determines if the name of the submitter is passed to GlobalLink. For privacy reason this should be
   * deactivated by default.
   */
  public static final String KEY_IS_SEND_SUBMITTER = "isSendSubmitter";

  /**
   * String value to control the expected incoming type of submission
   * instructions (also known as: comment). For details and possible values see
   * {@link GCSubmissionInstructionType}.
   */
  public static final String KEY_SUBMISSION_INSTRUCTION_TYPE = "submissionInstructionType";

  /**
   * Default type for facades. Will be used also if unset or if the type
   * cannot be parsed/is unknown.
   */
  static final String VALUE_TYPE_DEFAULT = "default";

  private GCConfigProperty() {
  }
}
