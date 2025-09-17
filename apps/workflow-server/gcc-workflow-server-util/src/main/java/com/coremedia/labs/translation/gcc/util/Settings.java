package com.coremedia.labs.translation.gcc.util;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.UnknownNullness;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents settings for GlobalLink, typically merged from different sources.
 * <p>
 * This record provides a convenient way to combine configuration data from
 * multiple sources while maintaining type safety and enabling deep merging
 * of nested maps. Later sources take precedence over earlier ones.
 *
 * @param properties merged properties from all sources
 * @since 2506.0.0-1
 */
public record Settings(@NonNull Map<String, Object> properties) {
  /**
   * The logger for this class.
   */
  @NonNull
  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * An empty settings instance with no properties.
   */
  @NonNull
  public static final Settings EMPTY = new Settings(Map.of());

  /**
   * Defines the global configuration path.
   * <p>
   * The path may denote a (settings) document as well as a folder that contains
   * settings documents to be respected for the GlobalLink settings.
   */
  @VisibleForTesting
  public static final String GLOBAL_CONFIGURATION_PATH = "/Settings/Options/Settings/Translation Services";

  /**
   * Defines the site-specific configuration path.
   * <p>
   * If a GlobalLink parameter should be different in a specific site,
   * then the path either denotes a site-specific (settings) document or a
   * folder that may contain settings documents to be used to determine the
   * GlobalLink settings.
   */
  @VisibleForTesting
  public static final String SITE_CONFIGURATION_PATH = "Options/Settings/Translation Services";

  /**
   * The maximum allowed nesting depth for map structures to prevent stack
   * overflow.
   * <p>
   * This limit protects against maliciously crafted or accidentally deeply
   * nested data. The limit is very conservative, as even settings with a depth
   * greater than two are rather unexpected. We allow a maximum depth of 10.
   */
  @VisibleForTesting
  public static final int MAX_DEPTH = 10;

  /**
   * The canonical constructor that sanitizes the provided properties map.
   * <p>
   * Removes invalid entries (null keys/values, empty collections/maps/strings),
   * recursively sanitizes nested maps/collections, and enforces depth limits.
   *
   * @param properties raw properties (will be defensively sanitized)
   */
  public Settings {
    requireNonNull(properties, "properties");
    properties = sanitizeMap(properties);
  }

  /**
   * Signals, if these settings are empty.
   *
   * @return {@code true} if settings are empty; {@code false} if not
   */
  public boolean isEmpty() {
    return properties.isEmpty();
  }

  /**
   * Returns a new {@code Settings} instance with all properties from the given
   * settings merged in using deep merge semantics.
   *
   * @param other another settings instance
   * @return a new merged {@code Settings}
   */
  @NonNull
  public Settings mergedWith(@NonNull Settings other) {
    requireNonNull(other, "other");
    if (other.isEmpty()) {
      return this;
    }
    Map<String, Object> merged = new HashMap<>(properties);
    other.properties.forEach((k, v) -> merged.merge(k, v, Settings::deepMerge));
    return new Settings(merged);
  }

  /**
   * Retrieves a value at the specified path within the settings properties.
   * <p>
   * This method navigates through nested maps using the provided path elements
   * in order. If any path element does not exist or if a non-map value is
   * encountered before reaching the end of the path, this method returns an
   * empty {@link Optional}.
   * <p>
   * This method provides safe navigation through potentially deeply nested
   * configuration structures without throwing exceptions for missing paths.
   *
   * @param path a list of path elements to navigate through nested maps
   * @return an {@link Optional} containing the value at the specified path, or
   * empty if not found
   */
  @NonNull
  public Optional<Object> at(@NonNull List<String> path) {
    Object value = properties;
    for (int i = 0; i < path.size() && value != null; i++) {
      String pathElement = path.get(i);
      if (value instanceof Map<?, ?> map) {
        value = map.get(pathElement);
      } else {
        value = null;
      }
    }
    return Optional.ofNullable(value);
  }

  /**
   * Retrieves a value at the specified path within the settings properties.
   * <p>
   * This is a convenience method that accepts the path as separate arguments
   * instead of a list. It navigates through nested maps using the provided path
   * elements in order. If any path element does not exist or if a non-map value
   * is encountered before reaching the end of the path, this method returns an
   * empty {@link Optional}.
   * <p>
   * This method provides safe navigation through potentially deeply nested
   * configuration structures without throwing exceptions for missing paths.
   *
   * @param firstElement  the first path element to navigate to
   * @param otherElements additional path elements to navigate through nested maps
   * @return an {@link Optional} containing the value at the specified path, or
   * empty if not found
   */
  @NonNull
  public Optional<Object> at(@NonNull String firstElement, @NonNull String... otherElements) {
    List<String> path = Stream.concat(
      Stream.of(firstElement),
      Stream.of(otherElements)
    ).toList();
    return at(path);
  }

  /**
   * Sanitizes a single raw source map.
   *
   * @param source raw input map
   * @return sanitized, potentially modified map
   */
  @NonNull
  private static Map<String, Object> sanitizeMap(@NonNull Map<String, Object> source) {
    return source.entrySet().stream()
      .filter(Settings::isValidEntry)
      .map(e -> sanitizeEntryValue(e, 0))
      .filter(Settings::isValidEntry)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Merges two values using deep merge semantics for maps. Expects both
   * maps to originate from already sanitized sources.
   * <p>
   * If both values are maps, they are merged recursively with the replacement
   * map's values taking precedence. For non-map values, the replacement value
   * completely overwrites the existing value.
   *
   * @param existing    the original value
   * @param replacement the new value to merge or overwrite with
   * @return the merged result
   */
  @NonNull
  private static Object deepMerge(@NonNull Object existing,
                                  @NonNull Object replacement) {
    if (existing instanceof Map<?, ?> && replacement instanceof Map<?, ?>) {
      // We know from previous processing that map keys are guaranteed to be strings
      Map<String, Object> existingMap = asStringKeyedMap((Map<?, ?>) existing);
      Map<String, Object> replacementMap = asStringKeyedMap((Map<?, ?>) replacement);

      return deepMergeMaps(existingMap, replacementMap);
    }
    return replacement;
  }

  /**
   * Performs a deep merge of two maps with string keys.
   *
   * @param existingMap    the existing map to merge into
   * @param replacementMap the new map whose values take precedence
   * @return a new map containing the merged result
   */
  @NonNull
  private static Map<String, Object> deepMergeMaps(@NonNull Map<String, Object> existingMap,
                                                   @NonNull Map<String, Object> replacementMap) {
    return replacementMap.entrySet().stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        Settings::deepMerge,
        () -> new HashMap<>(existingMap)
      ));
  }

  /**
   * Performs an unchecked cast of a raw map to a string-keyed map.
   * <p>
   * This cast is safe because all maps have been validated to contain only
   * string keys during the filtering process.
   *
   * @param rawMap the raw map to cast
   * @return the map cast to a string-keyed type
   */
  @SuppressWarnings("unchecked")
  @NonNull
  private static Map<String, Object> asStringKeyedMap(@NonNull Map<?, ?> rawMap) {
    return (Map<String, Object>) rawMap;
  }

  /**
   * Sanitizes an entry's value by recursively filtering map entries if the
   * value is a map.
   * <p>
   * For map values, this method ensures that only entries with valid keys and
   * values are included. Non-map values are returned unchanged. Nesting depth
   * is limited to prevent stack overflow from malicious data.
   *
   * @param entry the entry whose value is to be sanitized
   * @param depth the current nesting depth
   * @param <K>   the type of the key
   * @param <V>   the type of the value
   * @return an entry with a sanitized value
   */
  @NonNull
  private static <K, V> Map.Entry<K, Object> sanitizeEntryValue(@NonNull Map.Entry<K, V> entry, int depth) {
    return Map.entry(entry.getKey(), sanitizeValue(entry.getValue(), depth));
  }

  /**
   * Sanitizes a value by recursively filtering map entries if the value is a
   * map.
   * <p>
   * For map values, this method ensures that only entries with valid keys and
   * values are included. Non-map values are returned unchanged. Nesting depth
   * is limited to prevent stack overflow from malicious data.
   *
   * @param value the value to sanitize
   * @param depth the current nesting depth
   * @return a sanitized value with filtered entries if it is a map
   */
  @UnknownNullness
  private static Object sanitizeValue(@UnknownNullness Object value, int depth) {
    if (value instanceof Map<?, ?> mapValue) {
      if (depth >= MAX_DEPTH) {
        LOG.warn("Depth limit ({}) exceeded. Truncating nested structure.", MAX_DEPTH);
        return Map.of(); // Return empty map to maintain type consistency
      }

      return mapValue.entrySet().stream()
        .filter(Settings::isValidEntry)
        .map(e -> sanitizeEntryValue(e, depth + 1))
        .filter(Settings::isValidEntry)
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
        ));
    }
    if (value instanceof Collection<?>) {
      if (depth >= MAX_DEPTH) {
        LOG.warn("Depth limit ({}) exceeded. Truncating nested structure.", MAX_DEPTH);
        return List.of();
      }
      return ((Collection<?>) value).stream()
        .map(e -> sanitizeValue(e, depth + 1))
        .filter(Settings::isValidValue)
        .toList();
    }
    return value;
  }

  /**
   * Determines whether a map entry should be included in the merge result.
   * <p>
   * An entry is considered valid if both its key and value pass their
   * respective validation checks.
   * <p>
   * This method is typically called twice in a stream: once before sanitizing
   * the entry, and once after. While the first call should especially prevent
   * keys or values from being {@code null} (which is prohibited in further
   * processing), the second call is meant as a final guard not to take
   * irrelevant data into account. For simplification, we apply the thorough
   * check for both calls.
   *
   * @param entry the map entry to evaluate
   * @return {@code true} if the entry should be included; {@code false} otherwise
   */
  private static boolean isValidEntry(@NonNull Map.Entry<?, ?> entry) {
    return entry.getKey() instanceof String && isValidValue(entry.getValue());
  }

  /**
   * Validates whether a value is acceptable for inclusion in the settings.
   * <p>
   * Values are rejected if they are null, empty strings, empty collections,
   * or empty maps. This helps maintain a clean configuration without
   * meaningless entries.
   *
   * @param value the value to validate
   * @return {@code true} if the value should be included; {@code false} otherwise
   */
  private static boolean isValidValue(@Nullable Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof String string) {
      return !string.isEmpty();
    }
    if (value instanceof Collection<?> collection) {
      return !collection.isEmpty();
    }
    if (value instanceof Map<?, ?> map) {
      return !map.isEmpty();
    }
    return true;
  }
}
