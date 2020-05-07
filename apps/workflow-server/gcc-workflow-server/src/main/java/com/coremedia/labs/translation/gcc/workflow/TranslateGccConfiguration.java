package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.translate.xliff.config.XliffExporterConfiguration;
import com.coremedia.cap.translate.xliff.config.XliffImporterConfiguration;
import com.coremedia.translate.item.TranslateItemConfiguration;
import com.coremedia.translate.workflow.DefaultTranslationWorkflowDerivedContentsStrategy;
import com.coremedia.translate.workflow.TranslationWorkflowDerivedContentsStrategy;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({
        XliffImporterConfiguration.class,
        XliffExporterConfiguration.class,
        TranslateItemConfiguration.class})
@PropertySource(value = "classpath:META-INF/coremedia/gcc-workflow.properties")
@DefaultAnnotation(NonNull.class)
public class TranslateGccConfiguration {

  /**
   * A strategy for extracting derived contents from the default translation.xml workflow definition.
   *
   * @return globalLinkTranslationWorkflowDerivedContentsStrategy
   */
  @Bean
  TranslationWorkflowDerivedContentsStrategy globalLinkTranslationWorkflowDerivedContentsStrategy(){
    DefaultTranslationWorkflowDerivedContentsStrategy globalLinkTranslationWorkflowDerivedContentsStrategy = new DefaultTranslationWorkflowDerivedContentsStrategy();
    globalLinkTranslationWorkflowDerivedContentsStrategy.setProcessDefinitionName("TranslationGlobalLink");

    return globalLinkTranslationWorkflowDerivedContentsStrategy;
  }

}
