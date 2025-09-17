package com.coremedia.labs.translation.gcc.util;

import com.coremedia.cap.common.CapObjectDestroyedException;
import com.coremedia.cap.common.CapPropertyDescriptor;
import com.coremedia.cap.common.NoSuchTypeException;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.struct.Struct;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import java.util.Map;

import static com.coremedia.cap.common.CapPropertyDescriptorType.STRUCT;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides settings from various origins, such as a Spring Context or
 * CoreMedia content items.
 *
 * @since 2506.0.0-1
 */
public enum SettingsSource {
  ;

  private static final Logger LOG = getLogger(lookup().lookupClass());

  /**
   * Default bean name that holds GlobalLink configuration properties.
   */
  public static final String GCC_CONFIGURATION_PROPERTIES_NAME = "gccConfigurationProperties";
  /**
   * Default content type that is expected to contain settings.
   */
  public static final String CT_SETTINGS = "CMSettings";
  /**
   * Default property that is expected to contain settings.
   */
  public static final String P_SETTINGS = "settings";
  /**
   * Root node for GlobalLink Connect settings in a {@link Struct}.
   * <p>
   * <strong>Type</strong>: {@code Struct}
   */
  public static final String KEY_GLOBALLINK_ROOT = "globalLink";

  /**
   * Creates settings from Spring bean with name
   * {@link #GCC_CONFIGURATION_PROPERTIES_NAME}.
   *
   * @param beanFactory the Spring bean factory
   * @return settings backed by the Spring bean; empty settings, if referenced
   * bean is unavailable
   */
  @NonNull
  public static Settings fromContext(@NonNull BeanFactory beanFactory) {
    return fromContext(beanFactory, GCC_CONFIGURATION_PROPERTIES_NAME);
  }

  /**
   * Creates settings from a Spring bean.
   *
   * @param beanFactory the Spring bean factory
   * @param beanName    name of the bean; expected to reference a
   *                    {@code Map<String, Object>}
   * @return settings backed by the Spring bean; empty settings, if referenced
   * bean is unavailable
   */
  @SuppressWarnings("unchecked")
  @VisibleForTesting
  @NonNull
  static Settings fromContext(@NonNull BeanFactory beanFactory,
                              @NonNull String beanName) {
    if (!beanFactory.containsBean(beanName)) {
      LOG.warn("{} not found in bean context.", beanName);
      return Settings.EMPTY;
    }
    return new Settings((Map<String, Object>) beanFactory.getBean(beanName, Map.class));
  }

  /**
   * Gets settings from given path at the site. The path may either denote
   * a folder to contain settings documents (not searched recursively) or
   * directly a single settings document.
   * <p>
   * This method uses the default settings content type {@link #CT_SETTINGS} and
   * property name {@link #P_SETTINGS}.
   *
   * @param site the site to search within
   * @param path the relative path from the site root
   * @return settings found (and possibly merged) from given site
   */
  @NonNull
  public static Settings fromPathAtSite(@NonNull Site site, @NonNull String path) {
    return fromPathAtSite(site, path, CT_SETTINGS, P_SETTINGS);
  }

  /**
   * Gets settings from given path at the site. The path may either denote
   * a folder to contain settings documents (not searched recursively) or
   * directly a single settings document.
   *
   * @param site                   the site to search within
   * @param path                   the relative path from the site root
   * @param settingsTypeName       the content type that holds settings
   * @param settingsDescriptorName the property that holds the settings struct
   * @return settings found (and possibly merged) from given site
   */
  @VisibleForTesting
  @NonNull
  public static Settings fromPathAtSite(@NonNull Site site,
                                        @NonNull String path,
                                        @NonNull String settingsTypeName,
                                        @NonNull String settingsDescriptorName) {
    return fromPath(site.getSiteRootFolder(), path, settingsTypeName, settingsDescriptorName);
  }

  /**
   * Gets settings from given path at the repository. The path may either denote
   * a folder to contain settings documents (not searched recursively) or
   * directly a single settings document.
   * <p>
   * This method uses the default settings content type {@link #CT_SETTINGS} and
   * property name {@link #P_SETTINGS}.
   *
   * @param repository the content repository to search within
   * @param path       the relative path from the repository root
   * @return settings found (and possibly merged) from repository
   */
  @NonNull
  public static Settings fromPath(@NonNull ContentRepository repository,
                                  @NonNull String path) {
    return fromPath(repository, path, CT_SETTINGS, P_SETTINGS);
  }

  /**
   * Gets settings from given path at the repository. The path may either denote
   * a folder to contain settings documents (not searched recursively) or
   * directly a single settings document.
   *
   * @param repository             the content repository to search within
   * @param path                   the relative path from the repository root
   * @param settingsTypeName       the content type that holds settings
   * @param settingsDescriptorName the property that holds the settings struct
   * @return settings found (and possibly merged) from repository
   */
  @VisibleForTesting
  @NonNull
  public static Settings fromPath(@NonNull ContentRepository repository,
                                  @NonNull String path,
                                  @NonNull String settingsTypeName,
                                  @NonNull String settingsDescriptorName) {
    return fromPath(repository.getRoot(), path, settingsTypeName, settingsDescriptorName);
  }

  /**
   * Gets settings from given path relative to the parent. The path may either
   * denote a folder to contain settings documents (not searched recursively) or
   * directly a single settings document.
   *
   * @param parent                 the parent content to search within
   * @param path                   the relative path from the parent
   * @param settingsTypeName       the content type that holds settings
   * @param settingsDescriptorName the property that holds the settings struct
   * @return settings found (and possibly merged) at given path
   */
  @VisibleForTesting
  @NonNull
  static Settings fromPath(@NonNull Content parent,
                           @NonNull String path,
                           @NonNull String settingsTypeName,
                           @NonNull String settingsDescriptorName) {
    try {
      Content content = parent.getChild(path);
      if (content == null) {
        LOG.debug("No content found for the given path {} at {}.", path, parent);
        return Settings.EMPTY;
      }
      return fromContent(content, settingsTypeName, settingsDescriptorName);
    } catch (CapObjectDestroyedException e) {
      LOG.debug("Failed accessing the given path {} at {}.", path, parent, e);
      return Settings.EMPTY;
    }
  }

  /**
   * Gets settings from given content. The content may either denote a folder
   * to contain settings documents (not searched recursively) or directly a
   * single settings document.
   *
   * @param content                the content to extract settings from
   * @param settingsTypeName       the content type that holds settings
   * @param settingsDescriptorName the property that holds the settings struct
   * @return settings found (and possibly merged)
   */
  @NonNull
  private static Settings fromContent(@NonNull Content content,
                                      @NonNull String settingsTypeName,
                                      @NonNull String settingsDescriptorName) {
    if (content.isDocument()) {
      return fromDocument(content, settingsTypeName, settingsDescriptorName);
    }
    return content.getChildrenWithType(settingsTypeName)
      .stream()
      .map(document -> fromDocument(document, settingsTypeName, settingsDescriptorName))
      .reduce(Settings::mergedWith)
      .orElse(Settings.EMPTY);
  }

  /**
   * Creates a settings from a document with defensive error handling.
   *
   * @param content                the document content, may be {@code null}
   * @param settingsTypeName       the content type that holds settings
   * @param settingsDescriptorName the property that holds the settings struct
   * @return settings found (and possibly merged)
   */
  @NonNull
  private static Settings fromDocument(@Nullable Content content,
                                       @NonNull String settingsTypeName,
                                       @NonNull String settingsDescriptorName) {
    return new Settings(defensiveFromDocument(content, settingsTypeName, settingsDescriptorName));
  }

  /**
   * Safely extracts GlobalLink configuration from a document's settings struct.
   *
   * @param content                the content document to extract from, may be {@code null}
   * @param settingsTypeName       the content type that holds settings
   * @param settingsDescriptorName the property that holds the settings struct
   * @return a map of GlobalLink settings, or an empty map if extraction fails
   */
  @NonNull
  private static Map<String, Object> defensiveFromDocument(@Nullable Content content,
                                                           @NonNull String settingsTypeName,
                                                           @NonNull String settingsDescriptorName) {
    try {
      if (!isIsValidSettingsDocument(content, settingsTypeName, settingsDescriptorName)) {
        return Map.of();
      }

      Struct settingsStruct = content.getStruct(settingsDescriptorName);
      CapPropertyDescriptor globalLinkDescriptor = settingsStruct.getType().getDescriptor(KEY_GLOBALLINK_ROOT);
      if (globalLinkDescriptor == null || globalLinkDescriptor.getType() != STRUCT || !globalLinkDescriptor.isAtomic()) {
        return Map.of();
      }

      Struct globalLinkSettingsStruct = settingsStruct.getStruct(KEY_GLOBALLINK_ROOT);
      if (globalLinkSettingsStruct == null) {
        return Map.of();
      }

      return globalLinkSettingsStruct.toNestedMaps();
    } catch (CapObjectDestroyedException e) {
      return Map.of();
    }
  }

  /**
   * Validates that the given content is a proper settings document with the
   * required structure.
   *
   * @param content                the content to validate, may be {@code null}
   * @param settingsTypeName       the content type that holds settings
   * @param settingsDescriptorName the property that holds the settings struct
   * @return {@code true} if the content is a valid settings document with a
   * settings struct, {@code false} otherwise
   */
  private static boolean isIsValidSettingsDocument(@Nullable Content content,
                                                   @NonNull String settingsTypeName,
                                                   @NonNull String settingsDescriptorName) {
    if (content == null || !content.isDocument() || content.isDestroyed()) {
      return false;
    }

    ContentType contentType = content.getType();
    if (!contentType.isSubtypeOf(requireSettingsType(content, settingsTypeName))) {
      return false;
    }

    CapPropertyDescriptor settingsDescriptor = contentType.getDescriptor(settingsDescriptorName);
    return settingsDescriptor != null && settingsDescriptor.getType() == STRUCT && settingsDescriptor.isAtomic();
  }

  /**
   * Retrieves the settings content type from the content's repository.
   *
   * @param content          the content whose repository to query
   * @param settingsTypeName the name of the content type that holds settings
   * @return the settings content type
   * @throws IllegalStateException if the settings type is not found
   */
  @NonNull
  private static ContentType requireSettingsType(@NonNull Content content,
                                                 @NonNull String settingsTypeName) {
    return requireSettingsType(content.getRepository(), settingsTypeName);
  }

  /**
   * Retrieves the settings content type from the repository.
   *
   * @param repository       the repository to query
   * @param settingsTypeName the name of the content type that holds settings
   * @return the settings content type
   * @throws NoSuchTypeException if the settings type is not found
   */
  @NonNull
  private static ContentType requireSettingsType(@NonNull ContentRepository repository,
                                                 @NonNull String settingsTypeName) {
    ContentType contentType = repository.getContentType(settingsTypeName);
    if (contentType == null) {
      throw new NoSuchTypeException(settingsTypeName);
    }
    return contentType;
  }
}
