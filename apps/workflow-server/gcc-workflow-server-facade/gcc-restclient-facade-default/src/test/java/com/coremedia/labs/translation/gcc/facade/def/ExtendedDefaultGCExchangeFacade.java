package com.coremedia.labs.translation.gcc.facade.def;

import org.gs4tr.gcc.restclient.model.GCFile;
import org.gs4tr.gcc.restclient.model.LocaleConfig;
import org.gs4tr.gcc.restclient.operation.Content;
import org.gs4tr.gcc.restclient.request.PageableRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * An extended variant of the default facade, tailored for testing purpose.
 */
@NullMarked
public class ExtendedDefaultGCExchangeFacade extends DefaultGCExchangeFacade {
  public ExtendedDefaultGCExchangeFacade(Map<String, Object> config) {
    super(config);
  }

  public static ExtendedDefaultGCExchangeFacade connect(Map<String, Object> config) {
    return new ExtendedDefaultGCExchangeFacade(config);
  }

  public List<String> fileTypes() {
    return connectorsConfig().getFileTypes();
  }

  public List<TranslationDirection> translationDirections() {
    return translationDirectionsStream().toList();
  }

  public Stream<TranslationDirection> translationDirectionsStream() {
    return connectorsConfig().getLanguageDirections().stream()
      .map(TranslationDirection::of);
  }

  public List<Locale> supportedSourceLocales() {
    return supportedSourceLocalesStream().toList();
  }

  public Stream<Locale> supportedSourceLocalesStream() {
    return supportedLocalesStream(LocaleConfig::getIsSource);
  }

  public List<Locale> supportedTargetLocales() {
    return supportedTargetLocalesStream().toList();
  }

  public Stream<Locale> supportedTargetLocalesStream() {
    return supportedLocalesStream(lc -> !lc.getIsSource());
  }

  public Stream<Locale> supportedLocalesStream(Predicate<LocaleConfig> localeConfigPredicate) {
    return connectorsConfig().getSupportedLocales().stream()
      .filter(localeConfigPredicate)
      .map(LocaleConfig::getLocaleLabel)
      // GCC REST Backend Bug Workaround: Locale contains/may contain trailing space.
      .map(String::trim)
      .map(Locale::forLanguageTag);
  }

  public long totalRecordsCount() {
    Content.ContentResponseData contentList = getDelegate().getContentList();
    return contentList.getTotalRecordsCount();
  }

  public List<UploadedFile> getContentList() {
    List<GCFile> rawContentList = getDelegate().getContentList(new PageableRequest(1L, totalRecordsCount())).getResponseData();
    return rawContentList.stream()
      .map(UploadedFile::of)
      .toList();
  }
}
