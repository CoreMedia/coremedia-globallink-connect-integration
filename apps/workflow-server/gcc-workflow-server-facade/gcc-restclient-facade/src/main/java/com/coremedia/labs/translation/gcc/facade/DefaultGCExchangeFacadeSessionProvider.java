package com.coremedia.labs.translation.gcc.facade;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Factory which, depending on given settings, will create either a
 * default communication channel to GCC, a mocked one or a disabled one.
 */
@NullMarked
public final class DefaultGCExchangeFacadeSessionProvider implements GCExchangeFacadeSessionProvider {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final GCExchangeFacadeSessionProvider INSTANCE = new DefaultGCExchangeFacadeSessionProvider();

  private final List<GCExchangeFacadeProvider> facadeProviders;

  private DefaultGCExchangeFacadeSessionProvider() {
    ServiceLoader<GCExchangeFacadeProvider> loader = ServiceLoader.load(GCExchangeFacadeProvider.class);
    facadeProviders = StreamSupport.stream(loader.spliterator(), false).toList();
  }

  /**
   * @implNote Uses property {@value GCConfigProperty#KEY_TYPE} to decide which
   * facade to instantiate.
   */
  @Override
  public GCExchangeFacade openSession(Map<String, Object> settings) {
    String facadeType = String.valueOf(settings.getOrDefault(GCConfigProperty.KEY_TYPE, GCConfigProperty.VALUE_TYPE_DEFAULT));
    LOG.debug("Identified facade type to use: {}", facadeType);
    GCExchangeFacadeProvider defaultFacadeProvider = null;
    for (GCExchangeFacadeProvider facadeProvider : facadeProviders) {
      if (facadeProvider.isApplicable(facadeType)) {
        LOG.debug("Found GCExchange facade provider: {}", facadeProvider);
        return facadeProvider.getFacade(settings);
      }
      if (facadeProvider.isDefault()) {
        defaultFacadeProvider = facadeProvider;
      }
    }
    if (defaultFacadeProvider == null) {
      throw new IllegalStateException("No GCExchange facade available as default/fallback.");
    }
    return defaultFacadeProvider.getFacade(settings);
  }

  /**
   * Provides a singleton instance of the factory.
   *
   * @return factory
   */
  public static GCExchangeFacadeSessionProvider defaultFactory() {
    return INSTANCE;
  }
}
