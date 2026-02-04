package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.Version;
import com.coremedia.cap.translate.xliff.XliffImportResultItem;
import com.coremedia.cap.translate.xliff.config.XliffImporterConfiguration;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionModel;
import com.coremedia.labs.translation.gcc.facade.GCSubmissionState;
import com.coremedia.labs.translation.gcc.facade.GCTaskModel;
import com.coremedia.translate.workflow.AsRobotUser;
import com.google.common.io.Resources;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import static com.coremedia.cap.translate.xliff.XliffImportIssueSeverity.MAJOR;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.NO_SUCH_PROPERTY;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.CT_SITE_CONTENT;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.LOCALE_PROPERTY;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.MASTER_PROPERTY;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.MASTER_VERSION_PROPERTY;
import static com.google.common.io.Resources.getResource;
import static java.lang.invoke.MethodHandles.lookup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

/**
 * Tests {@link DownloadFromGlobalLinkAction}.
 */
@SpringJUnitConfig(classes = DownloadFromGlobalLinkActionTest.LocalConfig.class)
@DirtiesContext(classMode = AFTER_CLASS)
@NullMarked
class DownloadFromGlobalLinkActionTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final Pattern REPLACE_TARGET_PATTERN = Pattern.compile("(?<attr>cmxliff:target)=\"(?<value>[^\"]*)\"");
  private static final Pattern REPLACE_ORIGINAL_PATTERN = Pattern.compile("(?<attr>original)=\"(?<value>[^\"]*)\"");

  private final DownloadFromGlobalLinkAction action;
  private final GCExchangeFacade gcExchangeFacade;

  private Version masterVersion;
  private Content targetContent;

  DownloadFromGlobalLinkActionTest(@Autowired DownloadFromGlobalLinkAction action,
                                   @Autowired GCExchangeFacade gcExchangeFacade) {
    this.action = action;
    this.gcExchangeFacade = gcExchangeFacade;
  }

  @BeforeEach
  void setUp(TestInfo testInfo,
             @Autowired ContentRepository repository) {
    Content masterContent =
      repository.createContentBuilder()
        .name("master_%s".formatted(testInfo.getDisplayName()))
        .nameTemplate()
        .type(CT_SITE_CONTENT)
        .property(LOCALE_PROPERTY, Locale.US.toLanguageTag())
        .create();
    masterVersion = masterContent.checkIn();
    targetContent =
      repository.createContentBuilder()
        .name("target_%s".formatted(testInfo.getDisplayName()))
        .nameTemplate()
        .type(CT_SITE_CONTENT)
        .property(LOCALE_PROPERTY, Locale.GERMANY.toLanguageTag())
        .property(MASTER_PROPERTY, List.of(masterContent))
        .property(MASTER_VERSION_PROPERTY, IdHelper.parseVersionId(masterVersion.getId()))
        .checkedIn()
        .create();
  }

  @Test
  void executeXliffDownload() {
    String xliff = readXliff(masterVersion, targetContent);

    mockXliffDownload(gcExchangeFacade, xliff);

    action.doExecuteGlobalLinkAction(new DownloadFromGlobalLinkAction.Parameters(1L, new HashSet<>(), false), r -> {
    }, gcExchangeFacade, new HashMap<>());
    assertThat(targetContent.getString("string")).isEqualTo("Lörem Ipsüm");
  }

  @Nested
  class SubmissionErrorHandling {
    @Test
    void shouldForwardSubmissionErrorAsIssue() {
      doReturn(
        GCSubmissionModel.builder(1L)
          .state(GCSubmissionState.IN_PRE_PROCESS)
          .error(true)
          .build())
        .when(gcExchangeFacade).getSubmission(anyLong());

      AtomicReference<DownloadFromGlobalLinkAction.@Nullable Result> resultHolder = new AtomicReference<>();
      Map<String, List<@Nullable Content>> issues = new HashMap<>();
      action.doExecuteGlobalLinkAction(new DownloadFromGlobalLinkAction.Parameters(1L, new HashSet<>(), false), resultHolder::set, gcExchangeFacade, issues);

      assertThat(issues).containsKey(GlobalLinkWorkflowErrorCodes.SUBMISSION_ERROR);
    }
  }

  @Nested
  @DisplayName("Tests error handling on XLIFF import.")
  @DirtiesContext(classMode = AFTER_CLASS)
  class XliffImportErrorHandling {
    @Test
    void importErrorInResult() {
      String xliff = readXliff(masterVersion, targetContent)
        .replace("property:string:string", "property:string:nosuchproperty");

      mockXliffDownload(gcExchangeFacade, xliff);

      AtomicReference<DownloadFromGlobalLinkAction.@Nullable Result> resultHolder = new AtomicReference<>();
      action.doExecuteGlobalLinkAction(new DownloadFromGlobalLinkAction.Parameters(1L, new HashSet<>(), false), resultHolder::set, gcExchangeFacade, new HashMap<>());
      DownloadFromGlobalLinkAction.Result result = resultHolder.get();

      assertThat(result)
        .isInstanceOfSatisfying(DownloadFromGlobalLinkAction.Result.class, r -> {
          assertThat(r.resultItems.values().stream().anyMatch(list -> {
            XliffImportResultItem resultItem = list.isEmpty() ? null : list.get(0);
            return resultItem != null && resultItem.getCode() == NO_SUCH_PROPERTY &&
              resultItem.getSeverity() == MAJOR && Objects.equals(resultItem.getContent(), targetContent) &&
              Objects.equals(resultItem.getProperty(), "nosuchproperty");
          })).isTrue();
          assertThat(r.resultItems).containsKey(1L);
        });
      assertThat(targetContent.getString("string")).isEmpty();
    }
  }

  private static void mockXliffDownload(GCExchangeFacade gcExchangeFacade, String xliff) {
    doAnswer((Answer<Boolean>) invocationOnMock -> {
      BiPredicate<InputStream, GCTaskModel> consumer = invocationOnMock.getArgument(1);
      ByteArrayInputStream inputStream = new ByteArrayInputStream(xliff.getBytes(StandardCharsets.UTF_8));
      consumer.test(inputStream, new GCTaskModel(1L, Locale.GERMANY));
      return true;
    }).when(gcExchangeFacade).downloadCompletedTasks(anyLong(), any());
    doReturn(GCSubmissionModel.builder(1L).state(GCSubmissionState.DELIVERED).build()).when(gcExchangeFacade).getSubmission(anyLong());
  }

  private static String readXliff(Version masterVersion, Content targetContent) {
    return readXliff(masterVersion.getId(), targetContent.getId());
  }

  private static String readXliff(String masterVersionId, String targetContentId) {
    try {
      String raw = Resources.toString(getResource(DownloadFromGlobalLinkActionTest.class, "LoremIpsum.xliff"), StandardCharsets.UTF_8);
      String result = REPLACE_TARGET_PATTERN.matcher(raw).replaceAll(String.format("${attr}=\"%s\"", targetContentId));
      result = REPLACE_ORIGINAL_PATTERN.matcher(result).replaceAll(String.format("${attr}=\"%s\"", masterVersionId));
      LOG.debug("Generated XLIFF: {}", result);
      return result;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Configuration
  @Import({
    GCExchangeFacadeConfiguration.class,
    SimpleMultiSiteConfiguration.class,
    XliffImporterConfiguration.class
  })
  static class LocalConfig {
    @Bean
    @Scope(SCOPE_SINGLETON)
    public DownloadFromGlobalLinkAction downloadFromGlobalLinkAction(ApplicationContext context) {
      return new MockedDownloadFromGlobalLinkAction(context);
    }
  }

  private static final class MockedDownloadFromGlobalLinkAction extends DownloadFromGlobalLinkAction {
    @Serial
    private static final long serialVersionUID = -4082795575498550151L;
    private final ApplicationContext applicationContext;

    private MockedDownloadFromGlobalLinkAction(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }

    @Override
    AsRobotUser getAsRobotUser() {
      AsRobotUser asRobotUser = mock(AsRobotUser.class);
      doCallRealMethod().when(asRobotUser).call(any());
      return asRobotUser;
    }
  }
}
