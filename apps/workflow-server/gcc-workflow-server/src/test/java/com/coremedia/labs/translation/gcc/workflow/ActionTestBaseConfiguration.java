package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.multisite.DefaultSiteModel;
import com.coremedia.cap.multisite.SiteModel;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.springframework.xml.ResourceAwareXmlBeanDefinitionReader;
import com.coremedia.translate.TranslatablePredicate;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;

import java.util.Arrays;
import java.util.Collection;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;

@Configuration
@Import(XmlRepoConfiguration.class)
@ImportResource(
  value = "classpath:/com/coremedia/cap/multisite/multisite-services.xml",
  reader = ResourceAwareXmlBeanDefinitionReader.class
)
@DefaultAnnotation(NonNull.class)
public class ActionTestBaseConfiguration {
  static final String MASTER_VERSION_PROPERTY = "extendedInt";
  static final String MASTER_PROPERTY = "extendedLink";
  static final String LOCALE_PROPERTY = "extendedString";
  static final String CONTENT_TYPE_NAME = "SimpleExtendedAll";
  static final String TRANSLATABLE_STRING_PROPERTY = "string";
  static final String TRANSLATABLE_RICHTEXT_PROPERTY = "richtext";
  static final String TRANSLATABLE_STRUCT_PROPERTY = "struct";

  private static final Collection<String> TRANSLATABLE_PROPERTIES = Arrays.asList(TRANSLATABLE_STRING_PROPERTY, TRANSLATABLE_RICHTEXT_PROPERTY, TRANSLATABLE_STRUCT_PROPERTY);

  @Scope(SCOPE_SINGLETON)
  @Bean
  public GCExchangeFacade gcExchangeFacade() {
    return Mockito.mock(GCExchangeFacade.class, invocation -> {
      throw new IllegalStateException("Unexpected method called on GCExchangeFacade Mock. You may require to extend your mock setup. Invocation: " + invocation);
    });
  }

  @Bean
  @Scope(SCOPE_SINGLETON)
  public TranslatablePredicate translatablePredicate() {
    return descriptors -> descriptors.stream().anyMatch(descriptor -> TRANSLATABLE_PROPERTIES.contains(descriptor.getName()));
  }

  /**
   * Provides a Site Model prepared for the simplified content type model provided
   * (per default) by the XML Repository. It contains a content-type "SimpleExtendedAll" with
   * for example properties "string", "int", and all of them duplicated with "extended" prefix,
   * so for example "extendedString", "extendedInt". We will use all extended properties for
   * the "multi-site" feature, while the normal properties remain for translation.
   *
   * @return dummy site model
   */
  @Scope(SCOPE_SINGLETON)
  @Bean
  public SiteModel siteModel() {
    DefaultSiteModel siteModel = new DefaultSiteModel();
    siteModel.setMasterVersionProperty(MASTER_VERSION_PROPERTY);
    siteModel.setMasterProperty(MASTER_PROPERTY);
    siteModel.setLocaleProperty(LOCALE_PROPERTY);
    siteModel.setTranslationWorkflowRobotUser("admin");
    return siteModel;
  }
}
