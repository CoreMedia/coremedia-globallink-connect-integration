package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.CapObjectDestroyedException;
import com.coremedia.cap.common.CapPropertyDescriptor;
import com.coremedia.cap.common.CapPropertyDescriptorType;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.struct.Struct;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A source for settings, such as provided by Spring Context, settings
 * read from content items, etc.
 */
@FunctionalInterface
public interface SettingsSource extends Supplier<Map<String, Object>> {
  String GCC_CONFIGURATION_PROPERTIES_NAME = "gccConfigurationProperties";
  String CT_SETTINGS = "CMSettings";
  String P_SETTINGS = "settings";

  @SuppressWarnings("unchecked")
  @NonNull
  static SettingsSource fromContext(@NonNull BeanFactory beanFactory) {
    return () -> Map.copyOf((Map<String, Object>) beanFactory.getBean(GCC_CONFIGURATION_PROPERTIES_NAME, Map.class));
  }

  @NonNull
  static Stream<SettingsSource> findAllAt(@NonNull Site site, @NonNull String path) {
    return findAllAt(site.getSiteRootFolder(), path);
  }

  @NonNull
  static Stream<SettingsSource> findAllAt(@NonNull ContentRepository repository, @NonNull String path) {
    return findAllAt(repository.getRoot(), path);
  }

  @NonNull
  static Stream<SettingsSource> findAllAt(@NonNull Content parent, @NonNull String path) {
    Content content = parent.getChild(path);
    if (content == null) {
      return Stream.empty();
    }
    return streamFromContent(content);
  }

  @NonNull
  private static Stream<SettingsSource> streamFromContent(@NonNull Content content) {
    if (content.isDocument()) {
      return Stream.of(fromDocument(content));
    }
    return streamChildDocuments(content);
  }

  @NonNull
  private static Stream<SettingsSource> streamChildDocuments(@NonNull Content parent) {
    try {
      return parent.getChildDocuments().stream()
        .map(SettingsSource::fromDocument);
    } catch (CapObjectDestroyedException e) {
      return Stream.empty();
    }
  }

  @NonNull
  private static SettingsSource fromDocument(@Nullable Content content) {
    return () -> defensiveFromDocument(content);
  }

  @NonNull
  private static Map<String, Object> defensiveFromDocument(@Nullable Content content) {
    try {
      if (content == null || !content.isDocument() || content.isDestroyed()) {
        return Map.of();
      }

      ContentType contentType = content.getType();
      if (!contentType.isSubtypeOf(requireSettingsType(content))) {
        return Map.of();
      }

      CapPropertyDescriptor settingsDescriptor = contentType.getDescriptor(P_SETTINGS);
      if (settingsDescriptor == null || settingsDescriptor.getType() != CapPropertyDescriptorType.STRUCT || !settingsDescriptor.isAtomic()) {
        return Map.of();
      }

      Struct settingsStruct = content.getStruct(P_SETTINGS);
      CapPropertyDescriptor globalLinkDescriptor = settingsStruct.getType().getDescriptor(GCConfigProperty.KEY_GLOBALLINK_ROOT);
      if (globalLinkDescriptor == null || globalLinkDescriptor.getType() != CapPropertyDescriptorType.STRUCT || !globalLinkDescriptor.isAtomic()) {
        return Map.of();
      }

      Struct globalLinkSettingsStruct = settingsStruct.getStruct(GCConfigProperty.KEY_GLOBALLINK_ROOT);
      if (globalLinkSettingsStruct == null) {
        return Map.of();
      }

      return globalLinkSettingsStruct.toNestedMaps();
    } catch (CapObjectDestroyedException e) {
      return Map.of();
    }
  }

  @NonNull
  private static ContentType requireSettingsType(@NonNull Content content) {
    return requireSettingsType(content.getRepository());
  }

  @NonNull
  private static ContentType requireSettingsType(@NonNull ContentRepository repository) {
    ContentType contentType = repository.getContentType(CT_SETTINGS);
    if (contentType == null) {
      throw new IllegalStateException("Required ContentType \"%s\". not found.".formatted(CT_SETTINGS));
    }
    return contentType;
  }
}
