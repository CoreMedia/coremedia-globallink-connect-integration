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
   * Username for GCC connection.
   */
  public static final String KEY_USERNAME = "username";

  /**
   * Password for GCC connection.
   */
  public static final String KEY_PASSWORD = "password"; // NOSONAR squid:S2068 this is not a hard-coded password

  /**
   * The API URL for GCC REST endpoint.
   */
  public static final String KEY_URL = "url";

  /**
   * Connector Key for GCC connection.
   */
  public static final String KEY_KEY = "key";

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
   * Default type for facades. Will be used also if unset or if the type
   * cannot be parsed/is unknown.
   */
  static final String VALUE_TYPE_DEFAULT = "default";

  private GCConfigProperty() {
  }
}
