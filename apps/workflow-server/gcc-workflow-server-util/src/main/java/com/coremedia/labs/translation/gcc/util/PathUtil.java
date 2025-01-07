package com.coremedia.labs.translation.gcc.util;

import com.google.common.base.Splitter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Path utilities.
 * <p>
 * In this context, "path" means a '/'-separated standard path, like a
 * Unix file system path or the path part of a URL.  PathUtil does NOT
 * cover special syntaxes like Windows file system paths.
 */
final class PathUtil {
  private static final char PATH_SEPARATOR = '/';
  private static final String PATH_SEPARATOR_STR = "/";
  private static final String PATH_PARENT = "..";
  private static final String PATH_SELF = ".";

  // static utility class
  private PathUtil() {}

  /**
   * Determine whether the given path, if normalized, starts with a .. segment.
   */
  static boolean isReferringToParent(String path) {
    List<String> segments = tokenizeAndNormalize(path);
    return !segments.isEmpty() && PATH_PARENT.equals(segments.get(0));
  }

  /**
   * Checks whether the path is absolute.
   */
  private static boolean isAbsolute(String path) {
    return path.startsWith(PATH_SEPARATOR_STR);
  }

  /**
   * Parse the path.
   * <ul>
   *   <li>Tokenize it</li>
   *   <li>Eliminate foo/.. occurrences</li>
   *   <li>In case the directory is wanted and the path does not end with "/" omit the last segment.</li>
   * </ul>
   */
  private static List<String> tokenizeAndNormalize(String path) {
    Iterable<String> split = Splitter.on(PATH_SEPARATOR).omitEmptyStrings().split(path);
    List<String> segments = StreamSupport.stream(split.spliterator(), false)
                                         .filter(s -> !(PATH_SELF.equals(s)))
                                         .collect(Collectors.toList());
    for (int i=1; i<segments.size(); ++i) {
      if (PATH_PARENT.equals(segments.get(i)) && !PATH_PARENT.equals(segments.get(i-1))) {
        segments.remove(i);
        segments.remove(i-1);
        i = Math.max(0, i-2);
      }
    }
    if (isAbsolute(path) && !segments.isEmpty() && PATH_PARENT.equals(segments.get(0))) {
      throw new IllegalArgumentException("Cannot resolve beyond root: " + path);
    }
    return segments;
  }
}
