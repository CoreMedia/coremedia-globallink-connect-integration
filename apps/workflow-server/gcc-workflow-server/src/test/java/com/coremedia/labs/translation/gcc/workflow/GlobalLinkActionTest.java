package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cache.EvaluationException;
import com.coremedia.cap.common.Blob;
import com.coremedia.cap.common.CapConnection;
import com.coremedia.cap.common.CapException;
import com.coremedia.cap.common.RepositoryNotAvailableException;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.PathHelper;
import com.coremedia.cap.errorcodes.CapErrorCodes;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.cap.workflow.Task;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.labs.translation.gcc.util.RetryDelay;
import com.coremedia.labs.translation.gcc.util.Settings;
import com.coremedia.labs.translation.gcc.util.SettingsSource;
import com.coremedia.rest.validation.Severity;
import com.coremedia.springframework.xml.ResourceAwareXmlBeanDefinitionReader;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.coremedia.labs.translation.gcc.util.RetryDelay.saturatedOf;
import static com.coremedia.labs.translation.gcc.workflow.GlobalLinkAction.DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringJUnitConfig(GlobalLinkActionTest.LocalConfig.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
class GlobalLinkActionTest {
  @NonNull
  private final MockedGlobalLinkAction globalLinkAction;
  @NonNull
  private final ObjectProvider<Site> siteProvider;
  @NonNull
  private final ObjectProvider<GlobalLinkConfigBuilder> globalLinkConfigBuilderProvider;
  @NonNull
  private final ContentRepository repository;

  GlobalLinkActionTest(@Autowired @NonNull MockedGlobalLinkAction globalLinkAction,
                       @Autowired @NonNull ObjectProvider<Site> siteProvider,
                       @Autowired @NonNull ObjectProvider<GlobalLinkConfigBuilder> globalLinkConfigBuilderProvider,
                       @Autowired @NonNull ContentRepository repository) {
    this.globalLinkAction = globalLinkAction;
    this.siteProvider = siteProvider;
    this.globalLinkConfigBuilderProvider = globalLinkConfigBuilderProvider;
    this.repository = repository;
  }

  @Nested
  class OpenSessionBehavior {
    @Test
    void shouldRespectMockFacadeType() {
      GCExchangeFacade facade = globalLinkAction.superOpenSession(Map.of(
        GCConfigProperty.KEY_URL, "https://example.org/",
        GCConfigProperty.KEY_API_KEY, "irrelevantApiKey",
        GCConfigProperty.KEY_KEY, "irrelevantKey",
        GCConfigProperty.KEY_TYPE, "mock"));
      assertThat(facade).isInstanceOf(MockedGCExchangeFacade.class);
    }
  }

  @Nested
  class DoExecuteBehavior {
    private Site masterSite;

    @BeforeEach
    void setUp() {
      masterSite = siteProvider.getObject();
    }

    @Nested
    class CmsOutageBehavior {
      @ParameterizedTest(name = "[{index}] {arguments}")
      @CsvSource(useHeadersInDisplayName = true, textBlock = """
        retryDelaySource
        context
        global
        site
        """)
      void shouldAlwaysTriggerRetryOnTemporaryCmsOutages(@NonNull String retryDelaySource) {
        int expectedRetryDelay;

        switch (retryDelaySource) {
          case "context":
            // From gcc-workflow.properties
            expectedRetryDelay = 60;
            break;
          case "global":
            expectedRetryDelay = 120;
            globalLinkConfigBuilderProvider.getObject()
              .atGlobal()
              .withRetryDelay("cms-retry-delay", Duration.ofSeconds(expectedRetryDelay))
              .build();
            break;
          case "site":
            expectedRetryDelay = 180;
            globalLinkConfigBuilderProvider.getObject()
              .atSite(masterSite)
              .withRetryDelay("cms-retry-delay", Duration.ofSeconds(expectedRetryDelay))
              .build();
            break;
          default:
            throw new IllegalStateException("Unknown retry delay source %s".formatted(retryDelaySource));
        }

        GlobalLinkAction.Parameters<Object> params =
          new GlobalLinkAction.Parameters<>(
            null,
            List.of(masterSite.getSiteIndicator()),
            0
          );
        globalLinkAction.onDoExecuteGlobalLinkAction(() -> {
          throw new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null);
        });
        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(params);

        assertThat(result)
          .satisfies(
            r -> assertThat(r.issues)
              .extracting(String::valueOf, InstanceOfAssertFactories.STRING)
              .contains(GlobalLinkWorkflowErrorCodes.CMS_COMMUNICATION_ERROR),
            r -> assertThat(r.remainingAutomaticRetries)
              .as("Should signal extraordinary state, thus, that we are not waiting for GCC but for the CMS.")
              .isEqualTo(Integer.MAX_VALUE),
            r -> assertThat(r.retryDelaySeconds).isGreaterThanOrEqualTo(expectedRetryDelay)
          );
      }
    }

    @Nested
    @ParameterizedClass
    @EnumSource(GlobalLinkConfigBuilder.RetryDelayMode.class)
    class RetryDelayBehavior {
      @NonNull
      private final GlobalLinkConfigBuilder.RetryDelayMode retryDelayMode;

      public RetryDelayBehavior(@NonNull GlobalLinkConfigBuilder.RetryDelayMode retryDelayMode) {
        this.retryDelayMode = retryDelayMode;
      }

      @ParameterizedTest(name = "[{index}] Retry Delay Key With Overridden Name = {0}")
      @ValueSource(booleans = {true, false})
      void shouldUseExpectedRetryDelayKey(boolean overrideName) {
        int expectedRetryDelay = 424;
        String retryDelayKey = overrideName ? "retry-delay-key-overridden" : DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY;
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelayMode(retryDelayMode)
          .withRetryDelay(retryDelayKey, Duration.ofSeconds(expectedRetryDelay))
          .build();

        int remainingAutomaticRetries = 3;

        GlobalLinkAction.Parameters<Object> params =
          new GlobalLinkAction.Parameters<>(
            null,
            List.of(masterSite.getSiteIndicator()),
            remainingAutomaticRetries
          );

        if (overrideName) {
          globalLinkAction.setOverrideGccRetryDelaySettingsKey(retryDelayKey);
        }

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(params);

        assertThat(result)
          .satisfies(
            r -> assertThat(r.remainingAutomaticRetries)
              .as("As we had no issues, remaining automatic retries should be (re)set to 0.")
              .isEqualTo(0),
            r -> assertThat(r.retryDelaySeconds)
              .as("Should use expected retry delay key %s (overridden: %s)", retryDelayKey, overrideName)
              .isEqualTo(expectedRetryDelay)
          );
      }

      @Test
      void shouldRespectAdaptedRetryDelayForGeneralOperation() {
        int retryDelayBase = 1234;
        long delayDivisor = 2L;
        RetryDelay expectedRetryDelay = new RetryDelay(Duration.ofSeconds(retryDelayBase).dividedBy(2L));

        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelayMode(retryDelayMode)
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(retryDelayBase))
          .build();

        globalLinkAction.adaptDelayForGeneralRetryBy(
          (rd, c) -> saturatedOf(rd.value().dividedBy(delayDivisor))
        );

        GlobalLinkAction.Parameters<Object> params =
          new GlobalLinkAction.Parameters<>(
            null,
            List.of(masterSite.getSiteIndicator()),
            0
          );

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(params);

        assertThat(result.retryDelaySeconds)
          .as("Should use expected adapted retry delay key (%d divided by %d)", retryDelayBase, delayDivisor)
          .isEqualTo(expectedRetryDelay.toSecondsInt());
      }
    }
  }

  @Nested
  class IssuesAsJsonStringBehavior {
    @Test
    void shouldSerializeContentIssuesToJsonAsExpected() {
      Content someContent = repository.createContentBuilder()
        .name("Some Content")
        .type("SimpleEmpty")
        .nameTemplate()
        .create();
      // Mock some test response that contains contents.
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
  }

  @Nested
  class IsRepositoryUnavailableExceptionBehavior {
    @ParameterizedTest
    @EnumSource(RepositoryUnavailableFixture.class)
    void shouldSignalAMatchOnRepositoryNotAvailableVariant(@NonNull RepositoryUnavailableFixture fixture) {
      Exception exception = fixture.exception();
      assertThat(GlobalLinkAction.isRepositoryUnavailableException(exception))
        .as("Should signal a match for: %s".formatted(exception))
        .isTrue();
    }

    @ParameterizedTest
    @EnumSource(NoRepositoryUnavailableFixture.class)
    void shouldSignalNoMatchOnIrrelevantException(@NonNull NoRepositoryUnavailableFixture fixture) {
      Exception exception = fixture.exception();
      assertThat(GlobalLinkAction.isRepositoryUnavailableException(exception))
        .as("Should signal no match for: %s".formatted(exception))
        .isFalse();
    }
  }

  /*
   * ---------------------------------------------------------------------------
   * Test Fixtures
   * ---------------------------------------------------------------------------
   */

  enum RepositoryUnavailableFixture {
    REPOSITORY_NOT_AVAILABLE_EXCEPTION(createRepositoryNotAvailableException()),
    CONTENT_REPOSITORY_UNAVAILABLE_CAP_EXCEPTION(new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null)),
    NESTED_CONTENT_REPOSITORY_UNAVAILABLE_CAP_EXCEPTION(new RuntimeException(new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null))),
    USER_REPOSITORY_UNAVAILABLE_CAP_EXCEPTION(new CapException("foo", CapErrorCodes.USER_REPOSITORY_UNAVAILABLE, null, null)),
    REPOSITORY_NOT_AVAILABLE_CAP_EXCEPTION(new CapException("foo", CapErrorCodes.REPOSITORY_NOT_AVAILABLE, null, null)),
    RNAE_CAUSING_CAP_EXCEPTION(cause -> new CapException("foo", null, null, cause)),
    RNAE_CAUSING_INVALID_PROPERTY_EXCEPTION(cause -> new InvalidPropertyException(Object.class, "foo", "bar", cause)),
    RNAE_CAUSING_INVOCATION_TARGET_EXCEPTION(InvocationTargetException::new),
    RNAE_CAUSING_EVALUATION_EXCEPTION(EvaluationException::new),
    RNAE_CAUSING_RUNTIME_EXCEPTION(RuntimeException::new),
    RNAE_CAUSING_NESTED_RUNTIME_EXCEPTION(cause -> new RuntimeException(new RuntimeException(cause))),
    /**
     * Observed during debugging when the content server was restarted while a corba call was made:
     *
     * <pre>{@code
     * org.omg.CORBA.OBJECT_NOT_EXIST: FINE: 02510002: The server ID in the target object key does not match the server key expected by the server  vmcid: OMG  minor code: 2  completed: No
     * }</pre>
     */
    CORBA_OBJECT_NOT_EXIST_ISSUE_EXCEPTION(new CapException("content", CapErrorCodes.UNEXPECTED_RUNTIME_EXCEPTION, null, new OBJECT_NOT_EXIST()));

    @NonNull
    private final Exception exception;

    RepositoryUnavailableFixture(@NonNull Function<RepositoryNotAvailableException, Exception> exceptionFunction) {
      exception = exceptionFunction.apply(createRepositoryNotAvailableException());
    }

    RepositoryUnavailableFixture(@NonNull Exception exception) {
      this.exception = exception;
    }

    @NonNull
    public Exception exception() {
      return exception;
    }

    @NonNull
    public static RepositoryNotAvailableException createRepositoryNotAvailableException() {
      return new RepositoryNotAvailableException("foo", null, null);
    }
  }

  enum NoRepositoryUnavailableFixture {
    RUNTIME_EXCEPTION(new RuntimeException()),
    NESTED_RUNTIME_EXCEPTION(new RuntimeException(new RuntimeException())),
    INVOCATION_TARGET_EXCEPTION_WITH_IRRELEVANT_CAUSE(new InvocationTargetException(new RuntimeException())),
    IRRELEVANT_CAP_EXCEPTION_NO_ERROR_CODE(new CapException("foo", null, null, null)),
    CAP_EXCEPTION_WITH_IRRELEVANT_ERROR_CODE(new CapException("foo", CapErrorCodes.CANNOT_READ_BLOB, null, null)),
    ;

    @NonNull
    private final Exception exception;

    NoRepositoryUnavailableFixture(@NonNull Exception exception) {
      this.exception = exception;
    }

    @NonNull
    public Exception exception() {
      return exception;
    }
  }

  /*
   * ---------------------------------------------------------------------------
   * Mocked Action
   * ---------------------------------------------------------------------------
   */

  private static final class MockedGlobalLinkAction extends GlobalLinkAction<Void, Void> {
    @Serial
    private static final long serialVersionUID = -288745610618179168L;
    private final ApplicationContext applicationContext;
    private final GCExchangeFacade gcExchangeFacade;
    @NonNull
    private Runnable onDoExecuteGlobalLinkAction = () -> {
      // No operation.
    };
    @Nullable
    private String overrideGccRetryDelaySettingsKey;
    @Nullable
    private BiFunction<? super RetryDelay, ? super AdaptDelayForGeneralRetryContext<Void, Void>, RetryDelay> retryDelayOperator;

    private MockedGlobalLinkAction(ApplicationContext applicationContext, GCExchangeFacade gcExchangeFacade) {
      super(true);
      this.applicationContext = applicationContext;
      this.gcExchangeFacade = gcExchangeFacade;
    }

    public void onDoExecuteGlobalLinkAction(@NonNull Runnable onDoExecuteGlobalLinkAction) {
      this.onDoExecuteGlobalLinkAction = onDoExecuteGlobalLinkAction;
    }

    public void adaptDelayForGeneralRetryBy(@NonNull BiFunction<? super RetryDelay, ? super AdaptDelayForGeneralRetryContext<Void, Void>, RetryDelay> retryDelayOperator) {
      this.retryDelayOperator = retryDelayOperator;
    }

    @Override
    @Nullable
    Void doExtractParameters(Task task) {
      return null;
    }

    @Override
    void doExecuteGlobalLinkAction(Void params, Consumer<? super Void> resultConsumer,
                                   GCExchangeFacade facade, Map<String, List<Content>> issues) {
      onDoExecuteGlobalLinkAction.run();
    }

    @Override
    @NonNull
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }

    @NonNull
    @Override
    RetryDelay adaptDelayForGeneralRetry(@NonNull RetryDelay originalRetryDelay,
                                         @NonNull AdaptDelayForGeneralRetryContext<Void, Void> context) {
      if (retryDelayOperator == null) {
        return super.adaptDelayForGeneralRetry(originalRetryDelay, context);
      }
      return retryDelayOperator.apply(originalRetryDelay, context);
    }

    @NonNull
    @Override
    GCExchangeFacade openSession(@NonNull Map<String, Object> settings) {
      return gcExchangeFacade;
    }

    @NonNull
    GCExchangeFacade superOpenSession(@NonNull Map<String, Object> settings) {
      return super.openSession(settings);
    }

    @NonNull
    @Override
    Settings withGlobalSettings(@NonNull Settings base, @NonNull ContentRepository repository) {
      // Allow to also use our test-content-types.
      return base.mergedWith(SettingsSource.fromPath(
        repository,
        Settings.GLOBAL_CONFIGURATION_PATH,
        "SimpleStruct", "value"));
    }

    @NonNull
    @Override
    Settings withSiteSettings(@NonNull Settings base, @NonNull Site site) {
      // Allow to also use our test-content-types.
      return base.mergedWith(SettingsSource.fromPathAtSite(
        site,
        Settings.SITE_CONFIGURATION_PATH,
        SimpleMultiSiteConfiguration.CT_SITE_CONTENT, "struct"));
    }

    public void setOverrideGccRetryDelaySettingsKey(@Nullable String overrideGccRetryDelaySettingsKey) {
      this.overrideGccRetryDelaySettingsKey = overrideGccRetryDelaySettingsKey;
    }

    @Override
    protected String getGCCRetryDelaySettingsKey() {
      if (overrideGccRetryDelaySettingsKey == null) {
        return super.getGCCRetryDelaySettingsKey();
      }
      return overrideGccRetryDelaySettingsKey;
    }

    @Nullable
    @Override
    Blob issuesAsJsonBlob(Map<String, List<Content>> issues) {
      return Mockito.mock(Blob.class, "issuesAsJsonBlob(%d): %s".formatted(
        issues.size(),
        issues.entrySet().stream()
          .map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue().stream().map(Content::getId).collect(Collectors.joining(","))))
          .collect(Collectors.joining(";"))
      ));
    }

  }

  /*
   * ---------------------------------------------------------------------------
   * Application Context Configuration
   * ---------------------------------------------------------------------------
   */

  @Configuration(proxyBeanMethods = false)
  @Import({XmlRepoConfiguration.class, SimpleMultiSiteConfiguration.class, GCExchangeFacadeConfiguration.class})
  @ImportResource(reader = ResourceAwareXmlBeanDefinitionReader.class)
  @PropertySource("classpath:META-INF/coremedia/gcc-workflow.properties")
  static class LocalConfig {
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Bean
    MockedGlobalLinkAction globalLinkAction(@NonNull ApplicationContext context,
                                            @NonNull CapConnection connection,
                                            @NonNull GCExchangeFacade gcExchangeFacade) {
      MockedGlobalLinkAction action = new MockedGlobalLinkAction(context, gcExchangeFacade);
      action.setConnection(connection);
      return action;
    }

    @ConfigurationProperties(prefix = "gcc")
    @Bean
    public Map<String, Object> gccConfigurationProperties() {
      return new HashMap<>();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public GlobalLinkConfigBuilder globalLinkConfigBuilder(@NonNull CapConnection connection) {
      return new GlobalLinkConfigBuilder(connection.getContentRepository(), connection.getStructService());
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Site site(@NonNull ContentRepository repository, @NonNull SitesService sitesService) {
      String randomId = UUID.randomUUID().toString();
      repository.createContentBuilder()
        .type("SimpleSite")
        .name(PathHelper.join(randomId, "Site"))
        .property(SimpleMultiSiteConfiguration.ID_PROPERTY, randomId)
        .property(SimpleMultiSiteConfiguration.NAME_PROPERTY, randomId)
        .property(SimpleMultiSiteConfiguration.LOCALE_PROPERTY, Locale.US.toLanguageTag())
        .checkedIn()
        .create();
      return Objects.requireNonNull(sitesService.getSite(randomId), "Failed to retrieve site: %%s%s.".formatted(randomId));
    }
  }
}
