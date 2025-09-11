package com.coremedia.labs.translation.gcc.util;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.multisite.Site;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.UnknownNullness;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
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
   * Creates a new {@link Settings} instance from the given properties.
   * <p>
   * The provided map will be sanitized to remove empty or invalid entries.
   *
   * @param properties the properties to create the settings from
   * @return a new, sanitized {@link Settings} instance
   */
  @NonNull
  public static Settings of(@NonNull Map<String, Object> properties) {
    // Use Builder for sanitizing entries.
    return builder()
      .source(() -> properties)
      .build();
  }

  /**
   * Creates a new builder instance for constructing settings.
   *
   * @return a new builder instance
   */
  @NonNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder that combines different sources to create a settings
   * representation.
   * <p>
   * Sources added later overwrite sources added earlier using deep merge
   * semantics. When merging maps, existing keys are preserved unless explicitly
   * overwritten by later sources.
   */
  public static class Builder {

    /**
     * A list of sources to merge properties from, in order of precedence
     * (highest precedence last).
     */
    @NonNull
    private final List<SettingsSource> sources = new ArrayList<>();

    /**
     * Adds a source that provides settings from Spring beans.
     *
     * @param beanFactory the bean factory to source settings from
     * @return this builder instance for method chaining
     */
    @NonNull
    public Builder beanSource(@NonNull BeanFactory beanFactory) {
      sources.add(SettingsSource.fromContext(beanFactory));
      return this;
    }

    /**
     * Adds a source that provides settings from the given site.
     *
     * @param site the site to source settings from
     * @return this builder instance for method chaining
     */
    @NonNull
    public Builder siteSource(@NonNull Site site) {
      SettingsSource.allAt(site, SITE_CONFIGURATION_PATH).forEach(this::source);
      return this;
    }

    /**
     * Adds a source that provides settings from the content repository.
     *
     * @param repository the repository to source settings from
     * @return this builder instance for method chaining
     */
    @NonNull
    public Builder repositorySource(@NonNull ContentRepository repository) {
      SettingsSource.allAt(repository, GLOBAL_CONFIGURATION_PATH).forEach(this::source);
      return this;
    }

    /**
     * Adds the given settings as a source to be merged.
     * <p>
     * Sources are processed in the order they are added, where later sources
     * take precedence for conflicting values. Sources are merged deeply,
     * meaning that if a value is a map, the same merge pattern is applied
     * recursively to nested maps. This allows for granular overriding of
     * specific nested properties.
     * <p>
     * Multiple calls to this method append additional sources, which will
     * override previously added sources for specific values at any given path.
     *
     * @param settings the settings to add as a source for merging
     * @return this builder instance for method chaining
     */
    @NonNull
    public Builder source(@NonNull Settings settings) {
      sources.add(settings::properties);
      return this;
    }

    /**
     * Adds the given source to be merged.
     * <p>
     * Sources are processed in the order they are added, where later sources
     * take precedence for conflicting values. Sources are merged deeply,
     * meaning that if a value is a map, the same merge pattern is applied
     * recursively to nested maps. This allows for granular overriding of
     * specific nested properties.
     * <p>
     * Multiple calls to this method append additional sources, which will
     * override previously added sources for specific values at any given path.
     *
     * @param source the source to add for merging
     * @return this builder instance for method chaining
     */
    @NonNull
    public Builder source(@NonNull SettingsSource source) {
      sources.add(source);
      return this;
    }

    /**
     * Adds the given sources to be merged.
     * <p>
     * Sources are processed in the order they are given, where later sources
     * take precedence for conflicting values. Sources are merged deeply,
     * meaning that if a value is a map, the same merge pattern is applied
     * recursively to nested maps. This allows for granular overriding of
     * specific nested properties.
     * <p>
     * Multiple calls to this method append additional sources, which will
     * override previously added sources for specific values at any given path.
     *
     * @param sources a list of sources to add for merging
     * @return this builder instance for method chaining
     */
    @NonNull
    public Builder sources(@NonNull List<SettingsSource> sources) {
      this.sources.addAll(sources);
      return this;
    }

    /**
     * Adds the given sources to be merged.
     * <p>
     * Sources are processed in the order they are given, where later sources
     * take precedence for conflicting values. Sources are merged deeply,
     * meaning that if a value is a map, the same merge pattern is applied
     * recursively to nested maps. This allows for granular overriding of
     * specific nested properties.
     * <p>
     * Multiple calls to this method append additional sources, which will
     * override previously added sources for specific values at any given path.
     *
     * @param sources an array of sources to add for merging
     * @return this builder instance for method chaining
     */
    @NonNull
    public Builder sources(@NonNull SettingsSource... sources) {
      return sources(Arrays.asList(sources));
    }

    /**
     * Creates the {@link Settings} object by merging all configured sources.
     * <p>
     * The merge process validates that all map keys are strings and filters
     * out {@code null} or empty values according to the configured rules.
     *
     * @return a merged {@link Settings} object containing properties from all
     * sources
     */
    @NonNull
    public Settings build() {
      // Must not use `of` as `of` uses the builder for sanitizing.
      return new Settings(mergedSources(sources));
    }
  }

  /**
   * Merges all sources into a single map of properties.
   * <p>
   * Sources are processed in order, with later sources taking precedence.
   * Only entries with valid keys and values are included in the result.
   * Map nesting is limited to {@value #MAX_DEPTH} levels to prevent stack
   * overflow.
   *
   * @param sources a list of sources to merge
   * @return a merged map containing all valid properties
   */
  @NonNull
  private static Map<String, Object> mergedSources(@NonNull List<SettingsSource> sources) {
    return sources.stream()
      .map(SettingsSource::get)
      .flatMap(map -> map.entrySet().stream())
      .filter(Settings::considerEntry)
      .map(e -> sanitizeEntryValue(e, 0))
      .filter(Settings::considerEntry)
      .collect(
        Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue,
          (existing, replacement) -> deepMerge(existing, replacement, 0)
        )
      );
  }

  /**
   * Merges two values using deep merge semantics for maps.
   * <p>
   * If both values are maps, they are merged recursively with the replacement
   * map's values taking precedence. For non-map values, the replacement value
   * completely overwrites the existing value. Nesting depth is limited to
   * prevent stack overflow.
   *
   * @param existing    the original value
   * @param replacement the new value to merge or overwrite with
   * @param depth       the current nesting depth
   * @return the merged result
   */
  @NonNull
  private static Object deepMerge(@NonNull Object existing,
                                  @NonNull Object replacement,
                                  int depth) {
    if (depth >= MAX_DEPTH) {
      LOG.warn("Depth limit ({}) exceeded during merge. Using replacement value.", MAX_DEPTH);
      return sanitizeValue(replacement, depth);
    }

    if (existing instanceof Map<?, ?> && replacement instanceof Map<?, ?>) {
      // We know from previous processing that map keys are guaranteed to be strings
      Map<String, Object> existingMap = asStringKeyedMap((Map<?, ?>) existing);
      Map<String, Object> replacementMap = asStringKeyedMap((Map<?, ?>) replacement);

      return deepMergeMaps(existingMap, replacementMap, depth);
    }
    return sanitizeValue(replacement, depth);
  }

  /**
   * Performs a deep merge of two maps with string keys.
   * <p>
   * The replacement map's entries are merged into the existing map. Null values
   * in the replacement map are ignored to preserve existing values. Nested maps
   * are merged recursively up to the maximum allowed depth.
   *
   * @param existingMap    the existing map to merge into
   * @param replacementMap the new map whose values take precedence
   * @param depth          the current nesting depth
   * @return a new map containing the merged result
   */
  @NonNull
  private static Map<String, Object> deepMergeMaps(@NonNull Map<String, Object> existingMap,
                                                   @NonNull Map<String, Object> replacementMap,
                                                   int depth) {
    return replacementMap.entrySet().stream()
      .filter(Settings::considerEntry)
      .map(e -> sanitizeEntryValue(e, depth + 1))
      .filter(Settings::considerEntry)
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (existing, replacement) -> deepMerge(existing, replacement, depth + 1),
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
    if (value instanceof Map<?, ?>) {
      if (depth >= MAX_DEPTH) {
        LOG.warn("Depth limit ({}) exceeded. Truncating nested structure.", MAX_DEPTH);
        return Map.of(); // Return empty map to maintain type consistency
      }

      Map<?, ?> map = (Map<?, ?>) value;
      return map.entrySet().stream()
        .filter(Settings::considerEntry)
        .map(e -> sanitizeEntryValue(e, depth + 1))
        .filter(Settings::considerEntry)
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
        .filter(Settings::considerValue)
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
  private static boolean considerEntry(@NonNull Map.Entry<?, ?> entry) {
    return considerKey(entry.getKey()) && considerValue(entry.getValue());
  }

  /**
   * Validates whether a key is acceptable for inclusion in the settings.
   * <p>
   * Only string keys are accepted to ensure type safety and consistent
   * property access patterns.
   *
   * @param value the key to validate
   * @return {@code true} if the key is a non-null string; {@code false}
   * otherwise
   */
  private static boolean considerKey(@UnknownNullness Object value) {
    return value instanceof String;
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
  private static boolean considerValue(@UnknownNullness Object value) {
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
