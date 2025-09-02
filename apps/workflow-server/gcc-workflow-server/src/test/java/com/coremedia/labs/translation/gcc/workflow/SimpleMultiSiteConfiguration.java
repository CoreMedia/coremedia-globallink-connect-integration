package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.multisite.DefaultSiteModel;
import com.coremedia.cap.multisite.SiteModel;
import com.coremedia.cap.multisite.impl.MultiSiteConfiguration;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.cap.test.xmlrepo.XmlUapiConfig;
import com.coremedia.translate.TranslatablePredicate;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Configuration(proxyBeanMethods = false)
@Import({
  XmlRepoConfiguration.class,
  MultiSiteConfiguration.class
})
public class SimpleMultiSiteConfiguration {
  /**
   * Content type recommended to be used within sites.
   */
  public static final String CT_SITE_CONTENT = "SimpleSiteContent";
  /**
   * String property marked as translatable by a predicate bean.
   */
  public static final String TRANSLATABLE_STRING_PROPERTY = "string";
  /**
   * Rich text property marked as translatable by a predicate bean.
   */
  public static final String TRANSLATABLE_RICHTEXT_PROPERTY = "richtext";
  /**
   * Struct property marked as translatable by a predicate bean.
   */
  public static final String TRANSLATABLE_STRUCT_PROPERTY = "struct";
  /**
   * Properties that will be marked as translatable by the predicate bean.
   */
  private static final Set<String> TRANSLATABLE_PROPERTIES = Set.of(
    TRANSLATABLE_STRING_PROPERTY,
    TRANSLATABLE_RICHTEXT_PROPERTY,
    TRANSLATABLE_STRUCT_PROPERTY
  );
  /**
   * Dummy site indicator type.
   */
  public static final String CT_SITE_INDICATOR = "SimpleSite";
  /**
   * Site ID property.
   */
  public static final String ID_PROPERTY = "id";
  /**
   * Site name property.
   */
  public static final String NAME_PROPERTY = "name";
  /**
   * Site root property.
   */
  public static final String ROOT_PROPERTY = "root";
  /**
   * Site manager group property.
   */
  public static final String SITE_MANAGER_GROUP_PROPERTY = "siteManagerGroup";
  /**
   * Site content's locale property.
   */
  public static final String LOCALE_PROPERTY = "locale";
  /**
   * Site content's master property.
   */
  public static final String MASTER_PROPERTY = "master";
  /**
   * Site content's master version property.
   */
  public static final String MASTER_VERSION_PROPERTY = "masterVersion";
  /**
   * Site content's ignore updates property.
   */
  public static final String IGNORE_UPDATES_PROPERTY = "ignoreUpdates";

  @Bean
  @Scope(BeanDefinition.SCOPE_SINGLETON)
  public static XmlUapiConfig xmlUapiConfig() {
    return XmlUapiConfig.builder()
      .build();
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public TranslatablePredicate translatablePredicate() {
    return descriptors -> descriptors.stream()
      .anyMatch(descriptor -> TRANSLATABLE_PROPERTIES.contains(descriptor.getName()));
  }

  @Bean
  @Scope(BeanDefinition.SCOPE_SINGLETON)
  public SiteModel siteModel(@NonNull ContentRepository contentRepository) {
    DefaultSiteModel siteModel = new DefaultSiteModel();

    ContentType siteIndicatorType = requireNonNull(
      contentRepository.getContentType(CT_SITE_INDICATOR),
      () -> "Content type %s must exist.".formatted(CT_SITE_INDICATOR)
    );

    Set<String> availableDescriptorNames = siteIndicatorType.getDescriptorsByName().keySet();
    Set<String> requiredDescriptorNames = Set.of(
      ID_PROPERTY,
      NAME_PROPERTY,
      ROOT_PROPERTY,
      SITE_MANAGER_GROUP_PROPERTY,
      LOCALE_PROPERTY,
      MASTER_PROPERTY,
      MASTER_VERSION_PROPERTY,
      IGNORE_UPDATES_PROPERTY
    );
    if (!availableDescriptorNames.containsAll(requiredDescriptorNames)) {
      Set<String> missing = new HashSet<>(requiredDescriptorNames);
      missing.removeAll(availableDescriptorNames);
      throw new IllegalStateException("Required descriptors not found: %s".formatted(missing));
    }

    siteModel.setSiteIndicatorDocumentType(CT_SITE_INDICATOR);
    siteModel.setIdProperty(ID_PROPERTY);
    siteModel.setNameProperty(NAME_PROPERTY);
    siteModel.setRootProperty(ROOT_PROPERTY);
    siteModel.setSiteManagerGroupProperty(SITE_MANAGER_GROUP_PROPERTY);

    siteModel.setLocaleProperty(LOCALE_PROPERTY);
    siteModel.setMasterProperty(MASTER_PROPERTY);
    siteModel.setMasterVersionProperty(MASTER_VERSION_PROPERTY);
    siteModel.setIgnoreUpdatesProperty(IGNORE_UPDATES_PROPERTY);

    siteModel.setRootFolderPathPattern("/{0}/{4}");
    return siteModel;
  }
}
