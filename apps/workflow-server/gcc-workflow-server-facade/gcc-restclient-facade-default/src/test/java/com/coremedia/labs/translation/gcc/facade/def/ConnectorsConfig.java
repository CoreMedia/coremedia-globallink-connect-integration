package com.coremedia.labs.translation.gcc.facade.def;

import org.gs4tr.gcc.restclient.model.LanguageDirection;
import org.gs4tr.gcc.restclient.model.LocaleConfig;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        case 0:
          throw new IllegalStateException("Should have been filtered before.");
        case 1:
          // No need to update. Our existing state may be the better match
          // already.
          continue;
        case 2:
          // This is what we hoped for, just a minimal set of target locales
          // that is greater than 1.
          return entry;
        default:
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
    return result;
  }

  public static ConnectorsConfig of(org.gs4tr.gcc.restclient.operation.ConnectorsConfig.ConnectorsConfigResponseData data) {
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

  private static List<Locale> supportedLocales(org.gs4tr.gcc.restclient.operation.ConnectorsConfig.ConnectorsConfigResponseData data,
                                               Predicate<LocaleConfig> localeConfigPredicate) {
    return data.getSupportedLocales().stream()
      .filter(localeConfigPredicate)
      .map(LocaleConfig::getLocaleLabel)
      .map(ConnectorsConfig::toLocale)
      .toList();
  }

  private static Map<Locale, List<Locale>> languageDirections(org.gs4tr.gcc.restclient.operation.ConnectorsConfig.ConnectorsConfigResponseData data) {
    Map<Locale, List<Locale>> result = new HashMap<>();
    List<LanguageDirection> languageDirections = data.getLanguageDirections();
    for (LanguageDirection languageDirection : languageDirections) {
      Locale source = toLocale(languageDirection.getSourceLocale());
      Locale target = toLocale(languageDirection.getTargetLocale());
      result.computeIfAbsent(source, l -> new ArrayList<>()).add(target);
    }
    return result;
  }

  private static Locale toLocale(String languageTag) {
    // GCC REST Backend Bug Workaround: Locale contains/may contain trailing space.
    return Locale.forLanguageTag(languageTag.trim());
  }
}
