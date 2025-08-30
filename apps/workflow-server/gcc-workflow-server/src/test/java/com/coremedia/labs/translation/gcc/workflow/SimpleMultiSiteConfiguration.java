package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.multisite.DefaultSiteModel;
import com.coremedia.cap.multisite.SiteModel;
import com.coremedia.cap.multisite.impl.MultiSiteConfiguration;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.cap.test.xmlrepo.XmlUapiConfig;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

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
  @Scope(BeanDefinition.SCOPE_SINGLETON)
  public SiteModel siteModel() {
    DefaultSiteModel siteModel = new DefaultSiteModel();

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
