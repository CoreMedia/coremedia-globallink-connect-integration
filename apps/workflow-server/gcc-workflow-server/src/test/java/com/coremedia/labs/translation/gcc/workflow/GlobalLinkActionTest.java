package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.cap.workflow.Task;
import com.coremedia.springframework.xml.ResourceAwareXmlBeanDefinitionReader;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
  classes = GlobalLinkActionTest.LocalConfig.class
)
@DirtiesContext(classMode = AFTER_CLASS)
class GlobalLinkActionTest {

  @Mock
  private Site site;

  @Autowired
  private GlobalLinkAction globalLinkAction;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Configuration
  @Import(XmlRepoConfiguration.class)
  @ImportResource(
    reader = ResourceAwareXmlBeanDefinitionReader.class
  )
  static class LocalConfig {
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Bean
    public GlobalLinkAction sendToGlobalLinkAction(ApplicationContext context) {
      return new MockedGlobalLinkAction(context);
    }
  }

  @Test
  void testOpenSession() {
    GCExchangeFacade facade = globalLinkAction.openSession(site);
    assertThat(facade).isNotNull();
    assertThat(facade).isInstanceOf(MockedGCExchangeFacade.class);
  }

  private static class MockedGlobalLinkAction extends GlobalLinkAction<Void, Void> {
    private static final long serialVersionUID = -288745610618179168L;
    private final ApplicationContext applicationContext;

    private MockedGlobalLinkAction(ApplicationContext applicationContext) {
      super(true);
      this.applicationContext = applicationContext;
    }

    @Override
    Void doExtractParameters(Task task) {
      return null;
    }

    @Override
    void doExecuteGlobalLinkAction(Void params, Consumer<? super Void> resultConsumer,
                                   GCExchangeFacade facade, Map<String, List<Content>> issues) {
    }

    @Override
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }

    @Override
    protected Map<String, Object> getGccSettings(Site site) {
      return ImmutableMap.of(
              GCConfigProperty.KEY_URL, "http://lorem.ipsum.fun/",
              GCConfigProperty.KEY_API_KEY, "abcd",
              GCConfigProperty.KEY_KEY, "012345",
              GCConfigProperty.KEY_TYPE, "mock");
    }
  }
}
