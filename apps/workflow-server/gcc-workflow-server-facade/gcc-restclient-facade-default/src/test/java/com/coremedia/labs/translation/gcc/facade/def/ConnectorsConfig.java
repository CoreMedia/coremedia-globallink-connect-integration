package com.coremedia.labs.translation.gcc.facade.def;

import org.gs4tr.gcc.restclient.model.LanguageDirection;
import org.gs4tr.gcc.restclient.model.LocaleConfig;
import org.gs4tr.gcc.restclient.operation.ConnectorsConfig.ConnectorsConfigResponseData;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * GlobalLink Connector Configuration Wrapper. Provides information for
 * debugging purpose, as well as supporting dynamically adapted tests to
 * your configured connector (like choosing relevant source and target
 * locales).
 */
@NullMarked
public record ConnectorsConfig(String name,
                               String type,
                               List<String> fileTypes,
                               List<String> workflows,
                               List<Locale> sourceLocales,
                               List<Locale> targetLocales,
                               Map<Locale, List<Locale>> languageDirections,
                               boolean multiSourceLocaleSupport,
                               List<String> jobStates,
                               List<String> taskStates,
                               List<String> submissionStates) {
  /**
   * For testing purpose, select any valid pair of source and target
   * language for a translation submission.
   *
   * @return the key denotes the source, the value the target language
   * @throws java.util.NoSuchElementException if no suitable language
   *                                          direction could be found
   */
  public Map.Entry<Locale, Locale> anyLanguageDirectionPair() {
    return languageDirections.entrySet()
      .stream()
      // Must have at least one target locale.
      .filter(e -> !e.getValue().isEmpty())
      .map(e -> Map.entry(e.getKey(), e.getValue().stream().findFirst().orElseThrow()))
      .findFirst()
      .orElseThrow();
  }

  /**
   * A relevant language direction with preferably multi target locales
   * (if available), where again preferring a low number of target locales
   * to decrease the complexity.
   *
   * @return relevant language direction for test
   */
  public Map.Entry<Locale, List<Locale>> anyLanguageDirection() {
    Map<Locale, List<Locale>> nonEmptyTargets = languageDirections().entrySet().stream()
      .filter(e -> !e.getValue().isEmpty())
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    Map.Entry<Locale, List<Locale>> result = nonEmptyTargets.entrySet().stream().findFirst().orElseThrow();

    // Satisfied if we just got two target locales.
    if (result.getValue().size() == 2) {
      return result;
    }

    for (Map.Entry<Locale, List<Locale>> entry : nonEmptyTargets.entrySet()) {
      int previousTargetCount = result.getValue().size();
      int currentTargetCount = entry.getValue().size();
      switch (currentTargetCount) {
        case 0 -> throw new IllegalStateException("Should have been filtered before.");
        case 1 -> {
          // No need to update. Our existing state may be the better match
          // already.
        }
        case 2 -> {
          // This is what we hoped for, just a minimal set of target locales
          // that is greater than 1.
          return entry;
        }
        default -> {
          if (previousTargetCount == 1) {
            // We can safely switch without getting "worse" in terms of
            // our preferred number of target locales.
            result = entry;
          } else {
            // previousTargetCount and currentTargetCount must both be greater
            // than 2, so we just choose the lower count.
            if (previousTargetCount > currentTargetCount) {
              result = entry;
            }
          }
        }
      }
    }
    return result;
  }

  /**
   * Create a wrapper from connector configuration response data as provided
   * by the GlobalLink Connector.
   *
   * @param data GlobalLink Connector response data for connector configuration
   * @return wrapper object
   */
  public static ConnectorsConfig of(ConnectorsConfigResponseData data) {
    List<Locale> sourceLocales = supportedLocales(data, LocaleConfig::getIsSource);
    List<Locale> targetLocales = supportedLocales(data, c -> !c.getIsSource());
    Map<Locale, List<Locale>> languageDirections = languageDirections(data);
    return new ConnectorsConfig(
      data.getConnectorName(),
      data.getConnectorType(),
      data.getFileTypes(),
      data.getWorkflows(),
      sourceLocales,
      targetLocales,
      languageDirections,
      Boolean.TRUE.equals(data.getIsMultiSourceLocaleSupported()),
      data.getAvailableStates().getJobStatuses().stream().sorted().toList(),
      data.getAvailableStates().getTaskStatuses().stream().sorted().toList(),
      data.getAvailableStates().getSubmissionStatuses().stream().sorted().toList()
    );
  }

  /**
   * Get all supported locales matching the given predicate.
   *
   * @param data                  connector response data to use
   * @param localeConfigPredicate predicate to apply
   * @return list of matching locales
   */
  private static List<Locale> supportedLocales(ConnectorsConfigResponseData data,
                                               Predicate<LocaleConfig> localeConfigPredicate) {
    return data.getSupportedLocales().stream()
      .filter(localeConfigPredicate)
      .map(LocaleConfig::getLocaleLabel)
      .map(ConnectorsConfig::toLocale)
      .toList();
  }

  /**
   * Get all language directions as a map from source locale to applicable
   * target locales.
   *
   * @param data connector response data to use
   * @return map of source to single target locale or multiple target locales
   */
  private static Map<Locale, List<Locale>> languageDirections(ConnectorsConfigResponseData data) {
    Map<Locale, List<Locale>> result = new HashMap<>();
    List<LanguageDirection> languageDirections = data.getLanguageDirections();
    for (LanguageDirection languageDirection : languageDirections) {
      Locale source = toLocale(languageDirection.getSourceLocale());
      Locale target = toLocale(languageDirection.getTargetLocale());
      result.computeIfAbsent(source, l -> new ArrayList<>()).add(target);
    }
    return result;
  }

  /**
   * Transforms a locale string from GCC backend to a representation as a
   * {@link Locale}. Applies a workaround for an observed issue, where
   * configured locales contained additional spaces, that break parsing.
   *
   * @param languageTag language tag from GCC backend
   * @return locale representation
   */
  private static Locale toLocale(String languageTag) {
    return Locale.forLanguageTag(languageTag.trim());
  }
}
