package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cache.EvaluationException;
import com.coremedia.cap.common.Blob;
import com.coremedia.cap.common.CapException;
import com.coremedia.cap.common.RepositoryNotAvailableException;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.errorcodes.CapErrorCodes;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.cap.workflow.Task;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.rest.validation.Severity;
import com.coremedia.springframework.xml.ResourceAwareXmlBeanDefinitionReader;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = GlobalLinkActionTest.LocalConfig.class
)
@DirtiesContext(classMode = AFTER_CLASS)
class GlobalLinkActionTest {

  private static final Blob CMS_ISSUES_BLOB = mock(Blob.class);

  @Mock
  private Site site;

  @Autowired
  private GlobalLinkAction<Void, Void> globalLinkAction;

  AutoCloseable closeable;

  @BeforeEach
  void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void tearDown() throws Exception {
    closeable.close();
  }

  @Configuration
  @Import(XmlRepoConfiguration.class)
  @ImportResource(reader = ResourceAwareXmlBeanDefinitionReader.class)
  @PropertySource("classpath:META-INF/coremedia/gcc-workflow.properties")
  static class LocalConfig {
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Bean
    public GlobalLinkAction<Void, Void> globalLinkAction(ApplicationContext context) {
      return new MockedGlobalLinkAction(context);
    }

    @ConfigurationProperties(prefix = "gcc")
    @Bean
    public Map<String, Object> gccConfigurationProperties() {
      return new HashMap<>();
    }

  }

  @Test
  void testOpenSession() {
    GCExchangeFacade facade = globalLinkAction.openSession(site);
    assertThat(facade).isNotNull();
    assertThat(facade).isInstanceOf(MockedGCExchangeFacade.class);
  }

  @Test
  void testRepositoryException() {
    GlobalLinkAction.Parameters<Object> params =
            new GlobalLinkAction.Parameters<>(null, null, 0);
    GlobalLinkAction<Void, Void> exceptingGlobalLinkAction = spy(globalLinkAction);
    doThrow(new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null))
            .when(exceptingGlobalLinkAction).getSitesService();
    doReturn(CMS_ISSUES_BLOB).when(exceptingGlobalLinkAction).issuesAsJsonBlob(anyMap());
    GlobalLinkAction.Result<Void> result = exceptingGlobalLinkAction.doExecute(params);
    assertThat(result.issues).isEqualTo(CMS_ISSUES_BLOB);
    assertThat(result.remainingAutomaticRetries).isEqualTo(Integer.MAX_VALUE);
    assertThat(result.retryDelaySeconds).isEqualTo(60);
  }

  @Test
  void testCheckedOutByOtherIssueSerialization(@Autowired ContentRepository repository) {
    Content someContent = repository.createContentBuilder()
            .name("Some Content")
            .type("SimpleEmpty")
            .nameTemplate()
            .create();
    Map<Severity, Map<String, List<Content>>> issues = Map.of(
            Severity.ERROR,
            Map.of(
                    CapErrorCodes.CHECKED_OUT_BY_OTHER,
                    List.of(someContent)
            )
    );
    // Failed with JsonIOException as described in CoreMedia/coremedia-globallink-connect-integration#61
    // on inappropriate type adapter registration. Requires `registerTypeHierarchyAdapter` for content items rather
    // than `registerTypeAdapter`.
    String actual = GlobalLinkAction.issuesAsJsonString(issues);
    assertThat(actual)
            .isEqualTo(
                    "{\"%s\":{\"%s\":[\"%s\"]}}".formatted(
                            Severity.ERROR,
                            CapErrorCodes.CHECKED_OUT_BY_OTHER,
                            someContent.getId()
                    )
            );
  }

  @Test
  void testRepositoryUnavailable_positive() {
    RepositoryNotAvailableException repositoryNotAvailableException = new RepositoryNotAvailableException("foo", null, null);
    CapException contentRepositoryUnvailableCapException = new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null);

    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(repositoryNotAvailableException));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(contentRepositoryUnvailableCapException));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new CapException("foo", CapErrorCodes.USER_REPOSITORY_UNAVAILABLE, null, null)));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new CapException("foo", CapErrorCodes.REPOSITORY_NOT_AVAILABLE, null, null)));

    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new CapException("foo", null, null, repositoryNotAvailableException)));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new InvalidPropertyException(Object.class, "foo", "bar", repositoryNotAvailableException)));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new InvocationTargetException(repositoryNotAvailableException)));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new EvaluationException(repositoryNotAvailableException)));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new RuntimeException(repositoryNotAvailableException)));
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new RuntimeException(new RuntimeException(repositoryNotAvailableException))));

    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new RuntimeException(new RuntimeException(contentRepositoryUnvailableCapException))));

    // observed during debugging when the content server was restarted while a corba call was made:
    // org.omg.CORBA.OBJECT_NOT_EXIST: FINE: 02510002: The server ID in the target object key does not match the server key expected by the server  vmcid: OMG  minor code: 2  completed: No
    assertTrue(GlobalLinkAction.isRepositoryUnavailableException(new CapException("content", CapErrorCodes.UNEXPECTED_RUNTIME_EXCEPTION, null, new OBJECT_NOT_EXIST())));
  }

  @Test
  void testRepositoryUnavailable_negative() {
    assertFalse(GlobalLinkAction.isRepositoryUnavailableException(new RuntimeException()));
    assertFalse(GlobalLinkAction.isRepositoryUnavailableException(new RuntimeException(new RuntimeException())));
    assertFalse(GlobalLinkAction.isRepositoryUnavailableException(new InvocationTargetException(new RuntimeException())));
    assertFalse(GlobalLinkAction.isRepositoryUnavailableException(new CapException("foo", null, null, null)));
    assertFalse(GlobalLinkAction.isRepositoryUnavailableException(new CapException("foo", CapErrorCodes.CANNOT_READ_BLOB, null, null)));
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
    @NonNull
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
