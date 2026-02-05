package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.common.CapConnection;
import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.content.Version;
import com.coremedia.cap.translate.xliff.config.XliffExporterConfiguration;
import com.coremedia.cap.user.User;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.translate.item.TranslateItemConfiguration;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.assertj.core.api.Condition;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.CT_SITE_CONTENT;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.LOCALE_PROPERTY;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.MASTER_PROPERTY;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.MASTER_VERSION_PROPERTY;
import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.TRANSLATABLE_STRING_PROPERTY;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

/**
 * Tests {@link SendToGlobalLinkAction}.
 */
@SpringJUnitConfig(SendToGlobalLinkActionTest.LocalConfig.class)
@DirtiesContext(classMode = AFTER_CLASS)
@NullMarked
class SendToGlobalLinkActionTest {

  @SuppressWarnings("unchecked")
  @Test
  void startTranslationJob(TestInfo testInfo,
                           @Autowired SendToGlobalLinkAction action,
                           @Autowired GCExchangeFacade gcExchangeFacade,
                           @Autowired CapConnection connection,
                           @Autowired User user) {
    ContentType contentType = requireNonNull(connection.getContentRepository().getContentType(CT_SITE_CONTENT), "Required content type not available.");

    long expectedSubmissionId = 42L;
    Locale masterLocale = Locale.US;
    Locale derivedLocale = Locale.GERMANY;

    String displayName = testInfo.getDisplayName();
    Content masterContent = contentType.createByTemplate("/", displayName, "{3} ({1})", ImmutableMap.<String,Object>builder()
            .put(LOCALE_PROPERTY, masterLocale.toLanguageTag())
            .put(TRANSLATABLE_STRING_PROPERTY, "Lorem Ipsum")
            .build()
    );
    Version masterVersion = masterContent.checkIn();
    List<ContentObject> masterContents = singletonList(masterContent);
    Content targetContent = contentType.createByTemplate("/", displayName, "{3} ({1})", ImmutableMap.<String, Object>builder()
      .put(LOCALE_PROPERTY, derivedLocale.toLanguageTag())
      .put(MASTER_PROPERTY, masterContents)
      .put(MASTER_VERSION_PROPERTY, IdHelper.parseVersionId(masterVersion.getId()))
      .build()
    );
    targetContent.checkIn();

    String expectedFileId = targetContent.getId();
    @Nullable String[] uploadedXliff = {null};

    Mockito.doAnswer(invocation -> readXliff(invocation, expectedFileId, uploadedXliff))
            .when(gcExchangeFacade)
            .uploadContent(anyString(), any(Resource.class), any(Locale.class));

    Mockito.doReturn(expectedSubmissionId).when(gcExchangeFacade).submitSubmission(anyString(), anyString(), any(ZonedDateTime.class), anyString(), anyString(), any(Locale.class), anyMap());

    List<Content> derivedContents = singletonList(targetContent);
    String comment = "Test";
    ZonedDateTime dueDate = ZonedDateTime.of(LocalDateTime.now(ZoneId.systemDefault()).plusDays(30L), ZoneId.systemDefault());
    String workflow = "pseudo translation";
    SendToGlobalLinkAction.Parameters params = new SendToGlobalLinkAction.Parameters(displayName, comment, derivedContents, masterContents, dueDate, workflow, user);
    AtomicReference<@Nullable String> resultHolder = new AtomicReference<>();
    action.doExecuteGlobalLinkAction(params, resultHolder::set, gcExchangeFacade, new HashMap<>());
    String submissionId = resultHolder.get();

    ArgumentCaptor<Map<String, List<Locale>>> contentMapCaptor = ArgumentCaptor.forClass(Map.class);
    ArgumentCaptor<Locale> masterLocaleCaptor = ArgumentCaptor.forClass(Locale.class);
    ArgumentCaptor<String> submitterCaptor = ArgumentCaptor.forClass(String.class);

    Mockito.verify(gcExchangeFacade).uploadContent(anyString(), any(Resource.class), eq(masterLocale));
    Mockito.verify(gcExchangeFacade).submitSubmission(anyString(), anyString(), any(ZonedDateTime.class), anyString(), submitterCaptor.capture(), masterLocaleCaptor.capture(), contentMapCaptor.capture());

    assertThat(uploadedXliff[0])
            .describedAs("XLIFF shall contain all relevant information.")
            .isNotNull()
            .contains("Lorem Ipsum")
            .contains(masterVersion.getId())
            .contains(targetContent.getId());
    assertThat(contentMapCaptor.getValue())
            .describedAs("All files shall be submitted with the correct target locales.")
            .containsOnlyKeys(expectedFileId)
            .hasValueSatisfying(new Condition<>("contains target locale de-DE") {
              @Override
              public boolean matches(List<Locale> value) {
                return value.size() == 1 && derivedLocale.equals(value.get(0));
              }
            });
    assertThat(masterLocaleCaptor.getValue()).isEqualTo(masterLocale);
    assertThat(submissionId).isEqualTo(String.valueOf(expectedSubmissionId));
    assertThat(submitterCaptor.getValue()).isEqualTo("admin");
  }

  private static Object readXliff(InvocationOnMock invocation, String expectedFileId, @Nullable String[] uploadedXliff) throws IOException {
    Resource resource = invocation.getArgument(1);
    byte[] bytes;
    try (InputStream stream = resource.getInputStream()) {
      bytes = ByteStreams.toByteArray(stream);
    }
    uploadedXliff[0] = new String(bytes, StandardCharsets.UTF_8);
    return expectedFileId;
  }

  @Configuration
  @Import({
    GCExchangeFacadeConfiguration.class,
    SimpleMultiSiteConfiguration.class,
    XliffExporterConfiguration.class,
    TranslateItemConfiguration.class
  })
  static class LocalConfig {
    @Scope(SCOPE_SINGLETON)
    @Bean
    public SendToGlobalLinkAction sendToGlobalLinkAction(ApplicationContext context) {
      return new MockedSendToGlobalLinkAction(context);
    }

    @Scope(SCOPE_SINGLETON)
    @Bean
    public User user() {
      User user = mock(User.class);
      when(user.getName()).thenReturn("admin");
      return user;
    }
  }

  private static final class MockedSendToGlobalLinkAction extends SendToGlobalLinkAction {
    @Serial
    private static final long serialVersionUID = -4082795575498550151L;
    private final ApplicationContext applicationContext;

    private MockedSendToGlobalLinkAction(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }
  }
}
