package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Types of incoming types of submission instructions (also known as: comment).
 * By default, {@link #TEXT_BMP} is assumed.
 * <p>
 * The flag handles optional transformation of the submission instructions,
 * as they are expected to be forwarded to GCC as HTML.
 * <p>
 * For configuration convenience, values may be provided in camel-case,
 * snake-case, or kebab-case and are handled case-insensitively.
 */
public enum GCSubmissionInstructionType {
  /**
   * Expects, that incoming submission instructions to be forwarded to GCC
   * are already in HTML format.
   * <p>
   * Characters outside the Basic Multilingual Plane (BMP) will be forwarded
   * as is, which may subsequently cause failures, if the GCC backend does
   * not support them.
   */
  HTML,
  /**
   * Expects, that incoming submission instructions to be forwarded to GCC
   * are plain text.
   * <p>
   * Characters outside the Basic Multilingual Plane (BMP) will be forwarded
   * as is, which may subsequently cause failures, if the GCC backend does
   * not support them.
   */
  TEXT,
  /**
   * Expects, that incoming submission instructions to be forwarded to GCC
   * are already in HTML format.
   * <p>
   * Characters outside the Basic Multilingual Plane (BMP) will be replaced
   * to plain ASCII.
   */
  HTML_BMP,
  /**
   * Expects, that incoming submission instructions to be forwarded to GCC
   * are plain text.
   * <p>
   * Characters outside the Basic Multilingual Plane (BMP) will be replaced
   * to plain ASCII.
   */
  TEXT_BMP,
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());
  private final String id;

  GCSubmissionInstructionType() {
    id = stripUnderscoresAndDashes(name());
  }

  @NonNull
  public String getId() {
    return id;
  }

  /**
   * Returns the type for the given string. Case-insensitive.
   *
   * @param type type as string
   * @return the parsed type, or {@link #TEXT_BMP} if the type is unknown/not
   * set
   */
  @NonNull
  public static GCSubmissionInstructionType fromString(@Nullable String type) {
    if (type == null || type.isBlank()) {
      return TEXT_BMP;
    }
    String alignedType = stripUnderscoresAndDashes(type.trim());
    for (GCSubmissionInstructionType value : values()) {
      if (value.getId().equalsIgnoreCase(alignedType)) {
        return value;
      }
    }
    LOG.warn("Unknown submission instruction type '{}'. Defaulting to '{}'.", type, TEXT_BMP);
    return TEXT_BMP;
  }

  /**
   * Used to eventually support camel-case, snake-case, and kebab-case.
   *
   * @param str string to strip underscores and dashes from
   * @return string without underscores and dashes
   */
  @NonNull
  private static String stripUnderscoresAndDashes(@NonNull String str) {
    return str.replace("_", "").replace("-", "");
  }
}
