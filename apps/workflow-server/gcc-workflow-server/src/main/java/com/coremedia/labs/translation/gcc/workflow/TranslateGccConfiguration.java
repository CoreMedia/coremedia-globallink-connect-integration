package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.translate.xliff.config.XliffExporterConfiguration;
import com.coremedia.cap.translate.xliff.config.XliffImporterConfiguration;
import com.coremedia.translate.item.TranslateItemConfiguration;
import com.coremedia.translate.workflow.DefaultTranslationWorkflowDerivedContentsStrategy;
import com.coremedia.translate.workflow.TranslationWorkflowDerivedContentsStrategy;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Configuration
@Import({
        XliffImporterConfiguration.class,
        XliffExporterConfiguration.class,
        TranslateItemConfiguration.class})
@PropertySource(value = "classpath:META-INF/coremedia/gcc-workflow.properties")
@DefaultAnnotation(NonNull.class)
public class TranslateGccConfiguration {

  /**
   * Post processor for list bean {@value TranslatableExpressionsExtender#TRANSLATE_XLIFF_TRANSLATABLE_EXPRESSIONS} to add additional
   * translatable expressions.
   *
   * @return post processor
   */
  @Bean
  static BeanPostProcessor translatableExpressionsExtender() {
    return new TranslatableExpressionsExtender();
  }

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

  /**
   * Post processor for list bean {@value #TRANSLATE_XLIFF_TRANSLATABLE_EXPRESSIONS} to add additional
   * translatable expressions.
   * <p>
   * Functionally, this is a copy of the <code>blueprint.translate.xliff.translatableExpressions</code>
   * customizer in the Blueprint's Studio configuration.  Take care to keep both in sync!
   */
  @DefaultAnnotation(NonNull.class)
  public static class TranslatableExpressionsExtender implements BeanPostProcessor {
    public static final String TRANSLATE_XLIFF_TRANSLATABLE_EXPRESSIONS = "translate.xliff.translatableExpressions";

    /**
     * Extends list bean {@value #TRANSLATE_XLIFF_TRANSLATABLE_EXPRESSIONS}.
     *
     * @param bean     bean
     * @param beanName name of the bean
     * @return possibly customized list bean for translatable expressions
     * @throws BeanNotOfRequiredTypeException if bean {@value #TRANSLATE_XLIFF_TRANSLATABLE_EXPRESSIONS} is not of required iterable type
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
      if (TRANSLATE_XLIFF_TRANSLATABLE_EXPRESSIONS.equals(beanName)) {
        if (bean instanceof Iterable) {
          List<String> beans = Lists.newArrayList((Iterable<String>) bean);
          String ctaCustomText = "CMLinkable.localSettings.callToActionCustomText";
          if (!beans.contains(ctaCustomText)) {
            beans.add(ctaCustomText);
          }
          return beans;
        }
        throw new BeanNotOfRequiredTypeException(beanName, Iterable.class, bean.getClass());
      }
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
      return bean;
    }
  }
}
