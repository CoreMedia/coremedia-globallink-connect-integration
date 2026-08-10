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
import com.coremedia.labs.translation.gcc.facade.GCFacadeCommunicationException;
import com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade;
import com.coremedia.labs.translation.gcc.util.RetryDelay;
import com.coremedia.labs.translation.gcc.util.Settings;
import com.coremedia.labs.translation.gcc.util.SettingsSource;
import com.coremedia.rest.validation.Severity;
import com.coremedia.springframework.xml.ResourceAwareXmlBeanDefinitionReader;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.coremedia.labs.translation.gcc.util.RetryDelay.saturatedOf;
import static com.coremedia.labs.translation.gcc.workflow.GlobalLinkAction.DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY;
import static java.util.Objects.requireNonNullElseGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@SpringJUnitConfig(GlobalLinkActionTest.LocalConfig.class)
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@NullMarked
class GlobalLinkActionTest {
  private final MockedGlobalLinkAction globalLinkAction;
  private final ObjectProvider<Site> siteProvider;
  private final ObjectProvider<GlobalLinkConfigBuilder> globalLinkConfigBuilderProvider;
  private final ContentRepository repository;

  GlobalLinkActionTest(@Autowired MockedGlobalLinkAction globalLinkAction,
                       @Autowired ObjectProvider<Site> siteProvider,
                       @Autowired ObjectProvider<GlobalLinkConfigBuilder> globalLinkConfigBuilderProvider,
                       @Autowired ContentRepository repository) {
    this.globalLinkAction = globalLinkAction;
    this.siteProvider = siteProvider;
    this.globalLinkConfigBuilderProvider = globalLinkConfigBuilderProvider;
    this.repository = repository;
  }

  @Nested
  class OpenSessionBehavior {
    @Test
    void shouldRespectMockFacadeType() {
      GCExchangeFacade facade = globalLinkAction.superOpenSession(new Settings(Map.of(
        GCConfigProperty.KEY_URL, "https://example.org/",
        GCConfigProperty.KEY_API_KEY, "irrelevantApiKey",
        GCConfigProperty.KEY_KEY, "irrelevantKey",
        GCConfigProperty.KEY_TYPE, "mock")));
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
      @SuppressWarnings("NullAway")
      // false-positive non-null assumption for generic parameter <P extends @Nullable Object> in GlobalLinkAction.Parameters<P>
      @ParameterizedTest(name = "[{index}] {arguments}")
      @CsvSource(useHeadersInDisplayName = true, textBlock = """
        retryDelaySource
        context
        global
        site
        """)
      void shouldAlwaysTriggerRetryOnTemporaryCmsOutages(String retryDelaySource) {
        int expectedRetryDelay;

        switch (retryDelaySource) {
          case "context" ->
            // From gcc-workflow.properties
            expectedRetryDelay = 60;
          case "global" -> {
            expectedRetryDelay = 120;
            globalLinkConfigBuilderProvider.getObject()
              .atGlobal()
              .withRetryDelay("cms-retry-delay", Duration.ofSeconds(expectedRetryDelay))
              .build();
          }
          case "site" -> {
            expectedRetryDelay = 180;
            globalLinkConfigBuilderProvider.getObject()
              .atSite(masterSite)
              .withRetryDelay("cms-retry-delay", Duration.ofSeconds(expectedRetryDelay))
              .build();
          }
          default -> throw new IllegalStateException("Unknown retry delay source %s".formatted(retryDelaySource));
        }

        GlobalLinkAction.Parameters<@Nullable Object> params =
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
      private final GlobalLinkConfigBuilder.RetryDelayMode retryDelayMode;

      public RetryDelayBehavior(GlobalLinkConfigBuilder.RetryDelayMode retryDelayMode) {
        this.retryDelayMode = retryDelayMode;
      }

      @SuppressWarnings("NullAway")
      // false-positive non-null assumption for generic parameter <P extends @Nullable Object> in GlobalLinkAction.Parameters<P>
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

        GlobalLinkAction.Parameters<@Nullable Object> params =
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

      @SuppressWarnings("NullAway")
      // false-positive non-null assumption for generic parameter <P extends @Nullable Object> in GlobalLinkAction.Parameters<P>
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
          rd -> saturatedOf(rd.value().dividedBy(delayDivisor))
        );

        GlobalLinkAction.Parameters<@Nullable Object> params =
          new GlobalLinkAction.Parameters<>(
            null,
            List.of(masterSite.getSiteIndicator()),
            0
          );

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(params);

        assertThat(result)
          .isNotNull()
          .extracting(r -> r.retryDelaySeconds)
          .as("Should use expected adapted retry delay key (%d divided by %d)", retryDelayBase, delayDivisor)
          .isEqualTo(expectedRetryDelay.toSecondsInt());
      }
    }

    @Nested
    class RetryJitterBehavior {
      private static final int BASE_RETRY_DELAY_SECONDS = 1800;

      @SuppressWarnings("NullAway")
      // false-positive non-null assumption for generic parameter <P extends @Nullable Object> in GlobalLinkAction.Parameters<P>
      private GlobalLinkAction.Parameters<@Nullable Object> parameters() {
        return new GlobalLinkAction.Parameters<>(
          null,
          List.of(masterSite.getSiteIndicator()),
          0
        );
      }

      @Test
      void shouldNotApplyJitterByDefault() {
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          .build();

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());

        assertThat(result)
          .isNotNull()
          .extracting(r -> r.retryDelaySeconds)
          .as("Should keep the retry delay unmodified, as jitter is disabled by default.")
          .isEqualTo(BASE_RETRY_DELAY_SECONDS);
      }

      @ParameterizedTest(name = "[{index}] jitterPercentage={arguments}")
      @ValueSource(ints = {0, 100})
      void shouldRespectDisabledOrExplicitJitterPercentage(int jitterPercentage) {
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          .withInteger(GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY, jitterPercentage)
          .build();

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());

        assertThat(result)
          .isNotNull()
          .extracting(r -> r.retryDelaySeconds)
          .as("Should stay within the jitter band for %d%%.", jitterPercentage)
          .satisfies(actual -> assertThat(actual)
            .isBetween(
              expectedLowerBound(jitterPercentage),
              expectedUpperBound(jitterPercentage)
            ));
      }

      @Test
      void shouldApplyJitterOnGeneralRetry() {
        int jitterPercentage = 50;
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          .withInteger(GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY, jitterPercentage)
          .build();

        assertSoftly(softly -> {
          for (int i = 0; i < 20; i++) {
            GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());
            softly.assertThat(result)
              .isNotNull()
              .extracting(r -> r.retryDelaySeconds)
              .asInstanceOf(InstanceOfAssertFactories.INTEGER)
              .isBetween(
                expectedLowerBound(jitterPercentage),
                expectedUpperBound(jitterPercentage)
              );
          }
        });
      }

      @Test
      void shouldApplyJitterOnGccCommunicationError() {
        int jitterPercentage = 50;
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          .withInteger(GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY, jitterPercentage)
          .build();

        globalLinkAction.onDoExecuteGlobalLinkAction(() -> {
          throw new GCFacadeCommunicationException("Simulated GCC communication error.");
        });

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());

        assertThat(result)
          .isNotNull()
          .satisfies(
            r -> assertThat(r.issues)
              .extracting(String::valueOf, InstanceOfAssertFactories.STRING)
              .contains(GlobalLinkWorkflowErrorCodes.GLOBAL_LINK_COMMUNICATION_ERROR),
            r -> assertThat(r.retryDelaySeconds)
              .as("Should apply jitter also on GCC communication errors.")
              .isBetween(
                expectedLowerBound(jitterPercentage),
                expectedUpperBound(jitterPercentage)
              )
          );
      }

      @Test
      void shouldApplyJitterOnCmsCommunicationError() {
        int jitterPercentage = 50;
        int cmsRetryDelaySeconds = 600;
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(GlobalLinkAction.CMS_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(cmsRetryDelaySeconds))
          .withInteger(GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY, jitterPercentage)
          .build();

        globalLinkAction.onDoExecuteGlobalLinkAction(() -> {
          throw new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null);
        });

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());

        assertThat(result)
          .isNotNull()
          .satisfies(
            r -> assertThat(r.issues)
              .extracting(String::valueOf, InstanceOfAssertFactories.STRING)
              .contains(GlobalLinkWorkflowErrorCodes.CMS_COMMUNICATION_ERROR),
            r -> assertThat(r.retryDelaySeconds)
              .as("Should apply jitter also on CMS communication errors.")
              .isBetween(
                expectedLowerBound(cmsRetryDelaySeconds, jitterPercentage),
                expectedUpperBound(cmsRetryDelaySeconds, jitterPercentage)
              )
          );
      }

      @Test
      void shouldSaturateJitterPercentageAboveMaximum() {
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          // Above maximum: expected to be saturated to 100%, thus, not to be
          // interpreted as jitter being disabled.
          .withInteger(GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY, 150)
          .build();

        assertSoftly(softly -> {
          for (int i = 0; i < 20; i++) {
            GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());
            softly.assertThat(result)
              .isNotNull()
              .extracting(r -> r.retryDelaySeconds)
              .asInstanceOf(InstanceOfAssertFactories.INTEGER)
              .as("Should behave as if 100%% jitter had been configured.")
              .isBetween(expectedLowerBound(100), expectedUpperBound(100));
          }
        });
      }

      @Test
      void shouldSaturateJitterPercentageBelowMinimum() {
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          // Below minimum: expected to be saturated to 0%, thus, to be
          // interpreted as jitter being disabled.
          .withInteger(GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY, -50)
          .build();

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());

        assertThat(result)
          .isNotNull()
          .extracting(r -> r.retryDelaySeconds)
          .as("Should behave as if jitter had been disabled.")
          .isEqualTo(BASE_RETRY_DELAY_SECONDS);
      }

      @ParameterizedTest(name = "[{index}] Jitter Key With Overridden Name = {0}")
      @ValueSource(booleans = {true, false})
      void shouldUseExpectedJitterSettingsKey(boolean overrideName) {
        int jitterPercentage = 50;
        String jitterKey = overrideName
          ? "retry-jitter-key-overridden"
          : GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY;

        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          .withInteger(jitterKey, jitterPercentage)
          .build();

        if (overrideName) {
          globalLinkAction.setOverrideGccRetryJitterSettingsKey(jitterKey);
        }

        assertSoftly(softly -> {
          boolean anyJitterApplied = false;
          for (int i = 0; i < 20; i++) {
            GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());
            softly.assertThat(result)
              .isNotNull()
              .extracting(r -> r.retryDelaySeconds)
              .asInstanceOf(InstanceOfAssertFactories.INTEGER)
              .as("Should use expected jitter settings key %s (overridden: %s).", jitterKey, overrideName)
              .isBetween(expectedLowerBound(jitterPercentage), expectedUpperBound(jitterPercentage));
            anyJitterApplied = anyJitterApplied
              || (result != null && result.retryDelaySeconds != BASE_RETRY_DELAY_SECONDS);
          }
          softly.assertThat(anyJitterApplied)
            .as("Should actually have applied jitter, thus, read the settings from key %s.", jitterKey)
            .isTrue();
        });
      }

      @ParameterizedTest(name = "[{index}] jitterValue={arguments}")
      @ValueSource(strings = {"lorem", "20%", "0.2"})
      void shouldDisableJitterOnUnparseableValue(String invalidJitterValue) {
        globalLinkConfigBuilderProvider.getObject()
          .atGlobal()
          .withRetryDelay(DEFAULT_GCC_RETRY_DELAY_SETTINGS_KEY, Duration.ofSeconds(BASE_RETRY_DELAY_SECONDS))
          .withString(GlobalLinkAction.DEFAULT_GCC_RETRY_JITTER_SETTINGS_KEY, invalidJitterValue)
          .build();

        GlobalLinkAction.Result<Void> result = globalLinkAction.doExecute(parameters());

        assertThat(result)
          .isNotNull()
          .extracting(r -> r.retryDelaySeconds)
          .as("Should fall back to disabled jitter for unparseable value '%s'.", invalidJitterValue)
          .isEqualTo(BASE_RETRY_DELAY_SECONDS);
      }

      private static int expectedLowerBound(int jitterPercentage) {
        return expectedLowerBound(BASE_RETRY_DELAY_SECONDS, jitterPercentage);
      }

      private static int expectedUpperBound(int jitterPercentage) {
        return expectedUpperBound(BASE_RETRY_DELAY_SECONDS, jitterPercentage);
      }

      /**
       * Lower bound of the expected jitter band. Rounds towards the more
       * tolerant value, as the exact rounding behavior of the applied jitter
       * is irrelevant here.
       *
       * @param baseSeconds      base retry delay in seconds
       * @param jitterPercentage configured jitter percentage
       * @return lower bound in seconds
       */
      private static int expectedLowerBound(int baseSeconds, int jitterPercentage) {
        return (int) Math.floor(baseSeconds * (1.0d - jitterPercentage / 100.0d));
      }

      /**
       * Upper bound of the expected jitter band. Rounds towards the more
       * tolerant value, as the exact rounding behavior of the applied jitter
       * is irrelevant here.
       *
       * @param baseSeconds      base retry delay in seconds
       * @param jitterPercentage configured jitter percentage
       * @return upper bound in seconds
       */
      private static int expectedUpperBound(int baseSeconds, int jitterPercentage) {
        return (int) Math.ceil(baseSeconds * (1.0d + jitterPercentage / 100.0d));
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
      Map<Severity, Map<String, List<@Nullable Content>>> issues = Map.of(
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
    void shouldSignalAMatchOnRepositoryNotAvailableVariant(RepositoryUnavailableFixture fixture) {
      Exception exception = fixture.exception();
      assertThat(GlobalLinkAction.isRepositoryUnavailableException(exception))
        .as("Should signal a match for: %s".formatted(exception))
        .isTrue();
    }

    @ParameterizedTest
    @EnumSource(NoRepositoryUnavailableFixture.class)
    void shouldSignalNoMatchOnIrrelevantException(NoRepositoryUnavailableFixture fixture) {
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
    REPOSITORY_NOT_AVAILABLE_EXCEPTION() {
      @Override
      Exception exception() {
        return createRepositoryNotAvailableException();
      }
    },
    CONTENT_REPOSITORY_UNAVAILABLE_CAP_EXCEPTION() {
      @Override
      Exception exception() {
        return new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null);
      }
    },
    NESTED_CONTENT_REPOSITORY_UNAVAILABLE_CAP_EXCEPTION() {
      @Override
      Exception exception() {
        return new RuntimeException(new CapException("foo", CapErrorCodes.CONTENT_REPOSITORY_UNAVAILABLE, null, null));
      }
    },
    USER_REPOSITORY_UNAVAILABLE_CAP_EXCEPTION() {
      @Override
      Exception exception() {
        return new CapException("foo", CapErrorCodes.USER_REPOSITORY_UNAVAILABLE, null, null);
      }
    },
    REPOSITORY_NOT_AVAILABLE_CAP_EXCEPTION() {
      @Override
      Exception exception() {
        return new CapException("foo", CapErrorCodes.REPOSITORY_NOT_AVAILABLE, null, null);
      }
    },
    RNAE_CAUSING_CAP_EXCEPTION() {
      @Override
      Exception exception() {
        return new CapException("foo", null, null, createRepositoryNotAvailableException());
      }
    },
    RNAE_CAUSING_INVALID_PROPERTY_EXCEPTION() {
      @Override
      Exception exception() {
        return new InvalidPropertyException(Object.class, "foo", "bar", createRepositoryNotAvailableException());
      }
    },
    RNAE_CAUSING_INVOCATION_TARGET_EXCEPTION() {
      @Override
      Exception exception() {
        return new InvocationTargetException(createRepositoryNotAvailableException());
      }
    },
    RNAE_CAUSING_EVALUATION_EXCEPTION() {
      @Override
      Exception exception() {
        return new EvaluationException(createRepositoryNotAvailableException());
      }
    },
    RNAE_CAUSING_RUNTIME_EXCEPTION() {
      @Override
      Exception exception() {
        return new RuntimeException(createRepositoryNotAvailableException());
      }
    },
    RNAE_CAUSING_NESTED_RUNTIME_EXCEPTION() {
      @Override
      Exception exception() {
        return new RuntimeException(new RuntimeException(createRepositoryNotAvailableException()));
      }
    },
    /**
     * Observed during debugging when the content server was restarted while a corba call was made:
     *
     * <pre>{@code
     * org.omg.CORBA.OBJECT_NOT_EXIST: FINE: 02510002: The server ID in the target object key does not match the server key expected by the server  vmcid: OMG  minor code: 2  completed: No
     * }</pre>
     */
    CORBA_OBJECT_NOT_EXIST_ISSUE_EXCEPTION() {
      @Override
      Exception exception() {
        return new CapException("content", CapErrorCodes.UNEXPECTED_RUNTIME_EXCEPTION, null, new OBJECT_NOT_EXIST());
      }
    };

    abstract Exception exception();

    static RepositoryNotAvailableException createRepositoryNotAvailableException() {
      return new RepositoryNotAvailableException("foo", null, null);
    }
  }

  enum NoRepositoryUnavailableFixture {
    RUNTIME_EXCEPTION() {
      @Override
      Exception exception() {
        return new RuntimeException();
      }
    },
    NESTED_RUNTIME_EXCEPTION() {
      @Override
      Exception exception() {
        return new RuntimeException(new RuntimeException());
      }
    },
    INVOCATION_TARGET_EXCEPTION_WITH_IRRELEVANT_CAUSE() {
      @Override
      Exception exception() {
        return new InvocationTargetException(new RuntimeException());
      }
    },
    IRRELEVANT_CAP_EXCEPTION_NO_ERROR_CODE() {
      @Override
      Exception exception() {
        return new CapException("foo", null, null, null);
      }
    },
    CAP_EXCEPTION_WITH_IRRELEVANT_ERROR_CODE() {
      @Override
      Exception exception() {
        return new CapException("foo", CapErrorCodes.CANNOT_READ_BLOB, null, null);
      }
    },
    ;

    abstract Exception exception();
  }

  /*
   * ---------------------------------------------------------------------------
   * Mocked Action
   * ---------------------------------------------------------------------------
   */

  static final class MockedGlobalLinkAction extends GlobalLinkAction<@Nullable Void, Void> {
    @Serial
    private static final long serialVersionUID = -288745610618179168L;
    private final ApplicationContext applicationContext;
    private final GCExchangeFacade gcExchangeFacade;
    private Runnable onDoExecuteGlobalLinkAction = () -> {
      // No operation.
    };
    @Nullable
    private String overrideGccRetryDelaySettingsKey;
    @Nullable
    private String overrideGccRetryJitterSettingsKey;
    @Nullable
    private UnaryOperator<RetryDelay> retryDelayOperator;

    private MockedGlobalLinkAction(ApplicationContext applicationContext, GCExchangeFacade gcExchangeFacade) {
      super(true);
      this.applicationContext = applicationContext;
      this.gcExchangeFacade = gcExchangeFacade;
    }

    private void onDoExecuteGlobalLinkAction(Runnable onDoExecuteGlobalLinkAction) {
      this.onDoExecuteGlobalLinkAction = onDoExecuteGlobalLinkAction;
    }

    private void adaptDelayForGeneralRetryBy(UnaryOperator<RetryDelay> retryDelayOperator) {
      this.retryDelayOperator = retryDelayOperator;
    }

    @Override
    @Nullable Void doExtractParameters(Task task) {
      return null;
    }

    @Override
    void doExecuteGlobalLinkAction(@Nullable Void params, Consumer<? super Void> resultConsumer,
                                   GCExchangeFacade facade, Map<String, List<@Nullable Content>> issues) {
      onDoExecuteGlobalLinkAction.run();
    }

    @Override
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }

    @Override
    RetryDelay adaptDelayForGeneralRetry(RetryDelay originalRetryDelay,
                                         Settings settings,
                                         Optional<Void> extendedResult,
                                         Map<String, List<@Nullable Content>> issues) {
      if (retryDelayOperator == null) {
        return super.adaptDelayForGeneralRetry(originalRetryDelay, settings, extendedResult, issues);
      }
      return retryDelayOperator.apply(originalRetryDelay);
    }

    @Override
    GCExchangeFacade openSession(Settings settings) {
      return gcExchangeFacade;
    }

    GCExchangeFacade superOpenSession(Settings settings) {
      return super.openSession(settings);
    }

    @Override
    Settings withGlobalSettings(Settings base, ContentRepository repository) {
      // Allow to also use our test-content-types.
      return base.mergedWith(SettingsSource.fromPath(
        repository,
        GLOBAL_CONFIGURATION_PATH,
        "SimpleStruct", "value"));
    }

    @Override
    Settings withSiteSettings(Settings base, Site site) {
      // Allow to also use our test-content-types.
      return base.mergedWith(SettingsSource.fromPathAtSite(
        site,
        SITE_CONFIGURATION_PATH,
        SimpleMultiSiteConfiguration.CT_SITE_CONTENT, "struct"));
    }

    private void setOverrideGccRetryDelaySettingsKey(@Nullable String overrideGccRetryDelaySettingsKey) {
      this.overrideGccRetryDelaySettingsKey = overrideGccRetryDelaySettingsKey;
    }

    @Override
    protected String getGCCRetryDelaySettingsKey() {
      return requireNonNullElseGet(overrideGccRetryDelaySettingsKey, super::getGCCRetryDelaySettingsKey);
    }

    private void setOverrideGccRetryJitterSettingsKey(@Nullable String overrideGccRetryJitterSettingsKey) {
      this.overrideGccRetryJitterSettingsKey = overrideGccRetryJitterSettingsKey;
    }

    @Override
    protected String getGCCRetryJitterSettingsKey() {
      return requireNonNullElseGet(overrideGccRetryJitterSettingsKey, super::getGCCRetryJitterSettingsKey);
    }

    @Override
    Blob issuesAsJsonBlob(Map<String, List<@Nullable Content>> issues) {
      return Mockito.mock(Blob.class, "issuesAsJsonBlob(%d): %s".formatted(
        issues.size(),
        issues.entrySet().stream()
          .map(entry -> String.format("%s: %s",
            entry.getKey(),
            entry.getValue().stream()
              .filter(Objects::nonNull)
              .map(Content::getId)
              .collect(Collectors.joining(","))))
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
    MockedGlobalLinkAction globalLinkAction(ApplicationContext context,
                                            CapConnection connection,
                                            GCExchangeFacade gcExchangeFacade) {
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
    public GlobalLinkConfigBuilder globalLinkConfigBuilder(CapConnection connection) {
      return new GlobalLinkConfigBuilder(connection.getContentRepository(), connection.getStructService());
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Site site(ContentRepository repository, SitesService sitesService) {
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
