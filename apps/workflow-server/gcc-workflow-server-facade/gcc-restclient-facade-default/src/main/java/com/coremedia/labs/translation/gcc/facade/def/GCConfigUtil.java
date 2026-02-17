package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCFacadeConfigException;
import com.coremedia.labs.translation.gcc.util.Settings;
import org.gs4tr.gcc.restclient.GCConfig;
import org.gs4tr.gcc.restclient.GCExchange;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility class for creating {@link GCConfig} instances from our internal
 * configuration representation ({@link Settings}). This class also takes care
 * of redirecting the logging of the GCC REST Java Client to SLF4J, so that all
 * logs from the GCC REST Java Client are properly integrated into our
 * application's logging system.
 *
 * @since 2512.0.0-1
 */
@NullMarked
enum GCConfigUtil {
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());
  /**
   * Some string, so GCC can identify the source of requests.
   */
  private static final String USER_AGENT = lookup().lookupClass().getPackage().getName();

  /**
   * Returns the provider for GCConfig based on the given
   * {@link GCConfig} configuration (sub-struct).
   *
   * @param config the {@code globalLink} configuration
   * @return the provider for {@link GCConfig}
   */
  static GCConfig fromGlobalLinkConfig(Settings config) {
    GCConfig gcConfig = GCConfig.builder()
      .apiUrl(requireString(config, GCConfigProperty.KEY_URL))
      .apiKey(requireString(config, GCConfigProperty.KEY_API_KEY))
      .connectorKey(requireString(config, GCConfigProperty.KEY_KEY))
      .userAgent(USER_AGENT)
      .build();

    redirectLogging(gcConfig);

    configureConnectionRetries(config, gcConfig);

    return gcConfig;
  }

  private static void configureConnectionRetries(Settings config, GCConfig gcConfig) {
    findInteger(config, GCConfigProperty.KEY_MAX_RETRIES_ON_SERVICE_UNAVAILABLE)
      .ifPresent(gcConfig::setMaxRetriesOnServiceUnavailable);
    findInteger(config, GCConfigProperty.KEY_MAX_RETRIES_ON_REQUEST_ERRORS)
      .ifPresent(gcConfig::setMaxRetriesOnRequestErrors);
  }

  /**
   * Redirects the logging of the given {@link GCConfig} to SLF4J. This is
   * necessary because the GCC REST Java Client uses {@code java.util.logging}
   * (JUL) for logging, which is not used in our application. By redirecting the
   * logging to SLF4J, we can ensure that all logs from the GCC REST Java Client
   * are properly integrated into our application's logging system.
   *
   * @param gcConfig the {@link GCConfig} to redirect the logging for
   * @see SLF4JHandler
   */
  private static void redirectLogging(GCConfig gcConfig) {
    // Redirect logging to SLF4j.
    gcConfig.setLogger(SLF4JHandler.getLogger(GCExchange.class));
    gcConfig.getLogger().finest("JUL Logging redirection to SLF4J: OK");
    LOG.trace("JUL Logging redirected to SLF4J.");
  }

  /**
   * Try to find a string value for the given key in the given config. Throws an
   * exception if the key is not present.
   *
   * @param config the config to search in
   * @param key    the key to search for
   * @return the string value for the given key
   * @throws GCFacadeConfigException if the key is not present in the config
   */
  private static String requireString(Settings config, String key) {
    return findString(config, key)
      .orElseThrow(() -> new GCFacadeConfigException("Configuration for %s is missing. Configuration : %s", key, config));
  }

  /**
   * Try to find a string value for the given key in the given config. Returns an
   * empty Optional if the key is not present.
   *
   * @param config the config to search in
   * @param key    the key to search for
   * @return an Optional containing the string value if found, or an empty
   * Optional if not found
   */
  private static Optional<String> findString(Settings config, String key) {
    return config.at(key)
      .map(String::valueOf);
  }

  /**
   * Try to find an integer value for the given key in the given config. Returns
   * an empty Optional if the key is not present or if the value cannot be
   * parsed as an integer.
   *
   * @param config the config to search in
   * @param key    the key to search for
   * @return an Optional containing the integer value if found and parsable, or
   * an empty Optional if not found or not parsable
   */
  @SuppressWarnings("NullableProblems") // false-positive in IntelliJ Idea
  private static Optional<Integer> findInteger(Settings config, String key) {
    return config.at(key)
      .map(GCConfigUtil::tryParse);
  }

  /**
   * Tries to parse an integer from the given value. If the value is already an
   * integer, it is returned as is. If the value is a string, an attempt is made
   * to parse an integer from the string. If the parsing fails or if the value
   * is of any other type, {@code null} is returned.
   *
   * @param value the value to parse an integer from; may be {@code null}
   * @return the parsed integer, or {@code null} on any issue
   */
  private static @Nullable Integer tryParse(@Nullable Object value) {
    switch (value) {
      case null -> {
        return null;
      }
      case Integer intValue -> {
        return intValue;
      }
      case String stringValue -> {
        try {
          return Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
          LOG.trace("Unable to parse integer from string {}.", stringValue, e);
          return null;
        }
      }
      default -> {
        LOG.trace("Unexpected type {} to parse integer from value: {}.", value.getClass(), value);
        return null;
      }
    }
  }
}
