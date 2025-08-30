package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.CapObjectDestroyedException;
import com.coremedia.cap.common.CapPropertyDescriptor;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.struct.Struct;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.coremedia.cap.common.CapPropertyDescriptorType.STRUCT;
import static com.coremedia.labs.translation.gcc.facade.GCConfigProperty.KEY_GLOBALLINK_ROOT;
import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A source for settings, such as provided by Spring Context, settings
 * read from content items, etc.
 */
@FunctionalInterface
public interface SettingsSource extends Supplier<Map<String, Object>> {
  /**
   * Default bean name that holds GlobalLink Configuration Properties.
   */
  String GCC_CONFIGURATION_PROPERTIES_NAME = "gccConfigurationProperties";
  /**
   * Default content type that is expected to contain settings.
   */
  String CT_SETTINGS = "CMSettings";
  /**
   * Default property that is expected to contain settings.
   */
  String P_SETTINGS = "settings";

  /**
   * Returns the logger for this class.
   * <p>
   * Used as private static method to not expose a constant, that must be
   * public. Internal caching in Logback ensures that we do not create a
   * logger instance again and again.
   *
   * @return the logger for this class
   */
  @NonNull
  private static Logger log() {
    return getLogger(lookup().lookupClass());
  }

  /**
   * Creates a settings source from Spring bean factory (aka Spring context).
   *
   * @param beanFactory the Spring bean factory
   * @return settings source backed by Spring context
   */
  @NonNull
  static SettingsSource fromContext(@NonNull BeanFactory beanFactory) {
    return fromContext(beanFactory, GCC_CONFIGURATION_PROPERTIES_NAME);
  }

  /**
   * Creates a settings source from Spring bean factory (aka Spring context).
   *
   * @param beanFactory the Spring bean factory
   * @param beanName    name of the ({@code Map<String, Object>}) bean
   * @return settings source backed by Spring context
   */
  @SuppressWarnings("unchecked")
  @NonNull
  static SettingsSource fromContext(@NonNull BeanFactory beanFactory,
                                    @NonNull String beanName) {
    return () -> {
      if (!beanFactory.containsBean(beanName)) {
        log().warn("{} not found in bean context.", beanName);
        return Map.of();
      }
      return Map.copyOf((Map<String, Object>) beanFactory.getBean(beanName, Map.class));
    };
  }

  /**
   * Retrieves all settings sources at the specified path within the site.
   *
   * @param site the site to search within
   * @param path the relative path from site root
   * @return list of settings sources found at the path
   */
  @NonNull
  static List<SettingsSource> allAt(@NonNull Site site, @NonNull String path) {
    return allAt(site, path, CT_SETTINGS, P_SETTINGS);
  }

  /**
   * Retrieves all settings sources at the specified path within the site.
   *
   * @param site                   the site to search within
   * @param path                   the relative path from site root
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return list of settings sources found at the path
   */
  @NonNull
  static List<SettingsSource> allAt(@NonNull Site site,
                                    @NonNull String path,
                                    @NonNull String settingsTypeName,
                                    @NonNull String settingsDescriptorName) {
    return allAt(site.getSiteRootFolder(), path, settingsTypeName, settingsDescriptorName);
  }

  /**
   * Retrieves all settings sources at the specified path within the repository.
   *
   * @param repository the content repository to search within
   * @param path       the relative path from repository root
   * @return list of settings sources found at the path
   */
  @NonNull
  static List<SettingsSource> allAt(@NonNull ContentRepository repository,
                                    @NonNull String path) {
    return allAt(repository, path, CT_SETTINGS, P_SETTINGS);
  }

  /**
   * Retrieves all settings sources at the specified path within the repository.
   *
   * @param repository             the content repository to search within
   * @param path                   the relative path from repository root
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return list of settings sources found at the path
   */
  @NonNull
  static List<SettingsSource> allAt(@NonNull ContentRepository repository,
                                    @NonNull String path,
                                    @NonNull String settingsTypeName,
                                    @NonNull String settingsDescriptorName) {
    return allAt(repository.getRoot(), path, settingsTypeName, settingsDescriptorName);
  }

  /**
   * Retrieves all settings sources at the specified path relative to a parent content.
   *
   * @param parent the parent content to search within
   * @param path   the relative path from parent
   * @return list of settings sources found at the path, empty if path not found
   */
  @NonNull
  static List<SettingsSource> allAt(@NonNull Content parent,
                                    @NonNull String path) {
    return allAt(parent, path, CT_SETTINGS, P_SETTINGS);
  }

  /**
   * Retrieves all settings sources at the specified path relative to a parent content.
   *
   * @param parent                 the parent content to search within
   * @param path                   the relative path from parent
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return list of settings sources found at the path, empty if path not found
   */
  @NonNull
  static List<SettingsSource> allAt(@NonNull Content parent,
                                    @NonNull String path,
                                    @NonNull String settingsTypeName,
                                    @NonNull String settingsDescriptorName) {
    try {
      Content content = parent.getChild(path);
      if (content == null) {
        log().debug("No content found for the given path {} at {}.", path, parent);
        return List.of();
      }
      return allAt(content, settingsTypeName, settingsDescriptorName);
    } catch (CapObjectDestroyedException e) {
      log().debug("Failed accessing the given path {} at {}.", path, parent, e);
      return List.of();
    }
  }

  /**
   * Retrieves settings sources from content, handling both documents and folders.
   *
   * @param content                the content to extract settings from
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return list containing a single document source or all child document sources
   */
  @NonNull
  private static List<SettingsSource> allAt(@NonNull Content content,
                                            @NonNull String settingsTypeName,
                                            @NonNull String settingsDescriptorName) {
    if (content.isDocument()) {
      return List.of(fromDocument(content, settingsTypeName, settingsDescriptorName));
    }
    return allChildDocumentsAt(content, settingsTypeName, settingsDescriptorName);
  }

  /**
   * Extracts settings sources from all child documents of the given parent folder.
   *
   * @param parent                 the parent folder content
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return list of settings sources from child documents
   */
  @NonNull
  private static List<SettingsSource> allChildDocumentsAt(@NonNull Content parent,
                                                          @NonNull String settingsTypeName,
                                                          @NonNull String settingsDescriptorName) {
    try {
      // It is ok to just access all child documents here, as the subsequent
      // `fromDocument` is tolerant handling (thus, ignoring) irrelevant
      // or unmatched documents.
      return parent.getChildDocuments().stream()
        .map(content -> fromDocument(content, settingsTypeName, settingsDescriptorName))
        .toList();
    } catch (CapObjectDestroyedException e) {
      return List.of();
    }
  }

  /**
   * Creates a settings source from a document, with defensive error handling.
   *
   * @param content                the document content, may be null
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return settings source that safely extracts configuration
   */
  @NonNull
  private static SettingsSource fromDocument(@Nullable Content content,
                                             @NonNull String settingsTypeName,
                                             @NonNull String settingsDescriptorName) {
    return () -> defensiveFromDocument(content, settingsTypeName, settingsDescriptorName);
  }

  /**
   * Safely extracts GlobalLink configuration from a document's settings struct.
   *
   * @param content                the content document to extract from, may be null
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return GlobalLink settings map, or an empty map if extraction fails
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
   * Validates that the given content is a proper settings document with the required structure.
   *
   * @param content                the content to validate, may be null
   * @param settingsTypeName       (parent) content-type that holds settings
   * @param settingsDescriptorName descriptor that holds settings
   * @return true if content is a valid settings document with a settings struct
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
   * Retrieves the CMSettings content type from the content's repository.
   *
   * @param content          the content whose repository to query
   * @param settingsTypeName (parent) content-type that holds settings
   * @return the settings content type
   * @throws IllegalStateException if the settings type is not found
   */
  @NonNull
  private static ContentType requireSettingsType(@NonNull Content content, @NonNull String settingsTypeName) {
    return requireSettingsType(content.getRepository(), settingsTypeName);
  }

  /**
   * Retrieves the CMSettings content type from the repository.
   *
   * @param repository       the repository to query
   * @param settingsTypeName (parent) content-type that holds settings
   * @return the settings content type
   * @throws IllegalStateException if the CMSettings type is not found
   */
  @NonNull
  private static ContentType requireSettingsType(@NonNull ContentRepository repository, @NonNull String settingsTypeName) {
    ContentType contentType = repository.getContentType(settingsTypeName);
    if (contentType == null) {
      throw new IllegalStateException("Required content type \"%s\". not found.".formatted(settingsTypeName));
    }
    return contentType;
  }
}
