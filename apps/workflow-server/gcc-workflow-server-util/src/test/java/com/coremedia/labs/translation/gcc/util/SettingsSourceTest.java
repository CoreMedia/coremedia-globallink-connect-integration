package com.coremedia.labs.translation.gcc.util;

import com.coremedia.cap.common.CapConnection;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.PathHelper;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructService;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.coremedia.labs.translation.gcc.util.SettingsSource.KEY_GLOBALLINK_ROOT;
import static com.coremedia.labs.translation.gcc.util.SimpleMultiSiteConfiguration.CT_SITE_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

/**
 * Tests {@link SettingsSource}.
 */
// After each test method also cleans up any created content to prevent
// collisions.
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
@SpringJUnitConfig(SettingsSourceTest.LocalConfig.class)
@NullMarked
class SettingsSourceTest {
  private final ContentRepository repository;
  private final StructService structService;

  SettingsSourceTest(@Autowired CapConnection connection,
                     @Autowired ContentRepository repository) {
    this.repository = repository;
    structService = connection.getStructService();
  }

  @Nested
  class FromContextBehavior {
    private final ApplicationContext context;

    public FromContextBehavior(@Autowired ApplicationContext context) {
      this.context = context;
    }

    @Test
    void shouldProvideDefaultPropertiesBeanFromContext() {
      Settings settings = SettingsSource.fromContext(context);
      assertThat(settings.properties()).containsEntry("key1", "value1");
    }

    @Test
    void shouldProvideGivenPropertiesBeanFromContext() {
      Settings settings = SettingsSource.fromContext(context, "gccConfigurationProperties2");
      assertThat(settings.properties()).containsEntry("key2", "value2");
    }
  }

  @Nested
  class FromPathAtSiteBehavior {
    private final ObjectProvider<Site> siteProvider;
    private final ContentRepository repository;
    private final StructService structService;
    private Site site;

    FromPathAtSiteBehavior(@Autowired ObjectProvider<Site> siteProvider,
                           @Autowired ContentRepository repository,
                           @Autowired CapConnection connection) {
      this.siteProvider = siteProvider;
      this.repository = repository;
      structService = connection.getStructService();
    }

    @BeforeEach
    void setUp() {
      site = siteProvider.getObject();
    }

    @Test
    void shouldProvideEmptyForUnavailableSettingsPath() {
      Settings settings = SettingsSource.fromPathAtSite(site, "settings");
      assertThat(settings.properties()).isEmpty();
    }

    @Test
    void shouldProvideSettingsFromSiteDocument() {
      Struct settingsStruct = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key", Integer.MAX_VALUE, "value")
        .build();

      Content siteRootFolder = site.getSiteRootFolder();
      repository.createContentBuilder()
        .parent(siteRootFolder)
        .type(CT_SITE_CONTENT)
        .name("settings")
        .property("struct", settingsStruct)
        .checkedIn()
        .create();

      Settings settings = SettingsSource.fromPathAtSite(site, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settings.properties()).containsEntry("key", "value");
    }

    @Test
    void shouldProvideSettingsFromSiteFolderContents() {
      Struct settingsStruct1 = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key1", Integer.MAX_VALUE, "value1")
        .build();
      Struct settingsStruct2 = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key2", Integer.MAX_VALUE, "value2")
        .build();

      Content siteRootFolder = site.getSiteRootFolder();
      repository.createContentBuilder()
        .parent(siteRootFolder)
        .type(CT_SITE_CONTENT)
        .name("settings/1")
        .property("struct", settingsStruct1)
        .checkedIn()
        .create();

      repository.createContentBuilder()
        .parent(siteRootFolder)
        .type(CT_SITE_CONTENT)
        .name("settings/2")
        .property("struct", settingsStruct2)
        .checkedIn()
        .create();

      Settings settings = SettingsSource.fromPathAtSite(site, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settings.properties())
        .hasSize(2)
        .containsEntry("key1", "value1")
        .containsEntry("key2", "value2");
    }
  }

  @Nested
  class FromPathAtRepositoryBehavior {
    private final ContentRepository repository;
    private final StructService structService;

    FromPathAtRepositoryBehavior(@Autowired ContentRepository repository,
                                 @Autowired CapConnection connection) {
      this.repository = repository;
      structService = connection.getStructService();
    }

    @Test
    void shouldProvideEmptyForUnavailableSettingsPath() {
      Settings settings = SettingsSource.fromPath(repository, "settings");
      assertThat(settings.properties()).isEmpty();
    }

    @Test
    void shouldProvideSettingsFromDocument() {
      Struct settingsStruct = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key", Integer.MAX_VALUE, "value")
        .build();

      repository.createContentBuilder()
        .type(CT_SITE_CONTENT)
        .name("settings")
        .property("struct", settingsStruct)
        .checkedIn()
        .create();

      Settings settings = SettingsSource.fromPath(repository, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settings.properties())
        .hasSize(1)
        .containsEntry("key", "value");
    }

    @Test
    void shouldProvideSettingsFromFolderContents() {
      Struct settingsStruct1 = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key1", Integer.MAX_VALUE, "value1")
        .build();
      Struct settingsStruct2 = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key2", Integer.MAX_VALUE, "value2")
        .build();

      repository.createContentBuilder()
        .type(CT_SITE_CONTENT)
        .name("settings/1")
        .property("struct", settingsStruct1)
        .checkedIn()
        .create();

      repository.createContentBuilder()
        .type(CT_SITE_CONTENT)
        .name("settings/2")
        .property("struct", settingsStruct2)
        .checkedIn()
        .create();

      Settings settings = SettingsSource.fromPath(repository, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settings.properties())
        .hasSize(2)
        .containsEntry("key1", "value1")
        .containsEntry("key2", "value2");
    }
  }

  @Nested
  class FromPathAtParentBehavior {
    private Content parent;

    @BeforeEach
    void setUp() {
      parent = repository.createContentBuilder()
        .folderType()
        .name("parent")
        .nameTemplate()
        .create();
    }

    @Test
    void shouldProvideEmptyForUnavailableSettingsPath() {
      Settings settings = SettingsSource.fromPath(
        parent,
        "settings",
        CT_SITE_CONTENT,
        "struct");
      assertThat(settings.properties()).isEmpty();
    }

    @Test
    void shouldProvideSettingsFromDocument() {
      Struct settingsStruct = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key", Integer.MAX_VALUE, "value")
        .build();

      repository.createContentBuilder()
        .parent(parent)
        .type(CT_SITE_CONTENT)
        .name("settings")
        .property("struct", settingsStruct)
        .checkedIn()
        .create();

      Settings settings = SettingsSource.fromPath(parent, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settings.properties())
        .hasSize(1)
        .containsEntry("key", "value");
    }

    @Test
    void ShouldProvideSettingsFromFolderContents() {
      Struct settingsStruct1 = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key1", Integer.MAX_VALUE, "value1")
        .build();
      Struct settingsStruct2 = structService.createStructBuilder()
        .at(KEY_GLOBALLINK_ROOT)
        .declareString("key2", Integer.MAX_VALUE, "value2")
        .build();

      repository.createContentBuilder()
        .parent(parent)
        .type(CT_SITE_CONTENT)
        .name("settings/1")
        .property("struct", settingsStruct1)
        .checkedIn()
        .create();

      repository.createContentBuilder()
        .parent(parent)
        .type(CT_SITE_CONTENT)
        .name("settings/2")
        .property("struct", settingsStruct2)
        .checkedIn()
        .create();

      Settings settings = SettingsSource.fromPath(parent, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settings.properties())
        .hasSize(2)
        .containsEntry("key1", "value1")
        .containsEntry("key2", "value2");
    }

    /**
     * As content based API is the base for all other behaviors (at a site,
     * at the repository), we add some more tests (only) here regarding the
     * robustness behavior.
     */
    @Nested
    class RobustnessBehavior {
      @Test
      void shouldProvideEmptyForEmptySettingsPath() {
        Settings settings = SettingsSource.fromPath(
          repository.getRoot(),
          "parent",
          CT_SITE_CONTENT,
          "struct"
        );
        assertThat(settings.properties()).isEmpty();
      }

      @Test
      void shouldProvideEmptyForUnavailableGlobalLinkSettings() {
        Struct settingsStruct = structService.emptyStruct();

        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .property("struct", settingsStruct)
          .checkedIn()
          .create();

        Settings settings = SettingsSource.fromPath(parent, "settings", CT_SITE_CONTENT, "struct");

        assertThat(settings.properties()).isEmpty();
      }

      @Test
      void shouldProvideEmptyForUnavailableStructProperty() {
        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .checkedIn()
          .create();

        Settings settings = SettingsSource.fromPath(parent, "settings", CT_SITE_CONTENT, "doesNotExist");

        assertThat(settings.properties()).isEmpty();
      }

      @Test
      void shouldProvideEmptyForNotStructProperty() {
        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .checkedIn()
          .create();

        Settings settings = SettingsSource.fromPath(parent, "settings", CT_SITE_CONTENT, "string");

        assertThat(settings.properties()).isEmpty();
      }

      @Test
      void shouldProvideEmptyForUnmatchedContentType() {
        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .checkedIn()
          .create();

        Settings settings = SettingsSource.fromPath(parent, "settings", "SimpleEmpty", "irrelevant");

        assertThat(settings.properties()).isEmpty();
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Import(SimpleMultiSiteConfiguration.class)
  static class LocalConfig {
    @Bean
    public Map<String, Object> gccConfigurationProperties() {
      return Map.of("key1", "value1");
    }

    @Bean
    public Map<String, Object> gccConfigurationProperties2() {
      return Map.of("key2", "value2");
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
