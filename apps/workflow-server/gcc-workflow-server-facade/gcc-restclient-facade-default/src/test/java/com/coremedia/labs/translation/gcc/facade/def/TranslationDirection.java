package com.coremedia.labs.translation.gcc.facade.def;

import org.gs4tr.gcc.restclient.model.LanguageDirection;

import java.util.Locale;

public record TranslationDirection(
  Locale source,
  Locale target
) {
  public static TranslationDirection of(LanguageDirection direction) {
    Locale sourceLocale = Locale.forLanguageTag(direction.getSourceLocale().trim());
    Locale targetLocale = Locale.forLanguageTag(direction.getTargetLocale().trim());
    return new TranslationDirection(sourceLocale, targetLocale);
  }
}
