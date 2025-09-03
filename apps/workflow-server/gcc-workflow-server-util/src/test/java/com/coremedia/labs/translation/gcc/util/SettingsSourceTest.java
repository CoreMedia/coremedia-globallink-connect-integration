package com.coremedia.labs.translation.gcc.util;

import com.coremedia.cap.common.CapConnection;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.content.PathHelper;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructService;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.assertj.core.api.InstanceOfAssertFactories;
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

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
class SettingsSourceTest {
  @NonNull
  private final ContentRepository repository;
  @NonNull
  private final StructService structService;

  SettingsSourceTest(@Autowired @NonNull CapConnection connection,
                     @Autowired @NonNull ContentRepository repository) {
    this.repository = repository;
    structService = connection.getStructService();
  }

  @Nested
  class FromContextBehavior {
    @NonNull
    private final ApplicationContext context;

    public FromContextBehavior(@Autowired @NonNull ApplicationContext context) {
      this.context = context;
    }

    @Test
    void shouldProvideDefaultPropertiesBeanFromContext() {
      SettingsSource source = SettingsSource.fromContext(context);
      assertThat(source)
        .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
        .containsEntry("key1", "value1");
    }

    @Test
    void shouldProvideGivenPropertiesBeanFromContext() {
      SettingsSource source = SettingsSource.fromContext(context, "gccConfigurationProperties2");
      assertThat(source)
        .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
        .containsEntry("key2", "value2");
    }
  }

  @Nested
  class AllAtSiteBehavior {
    @NonNull
    private final ObjectProvider<Site> siteProvider;
    @NonNull
    private final ContentRepository repository;
    @NonNull
    private final StructService structService;
    private Site site;

    AllAtSiteBehavior(@Autowired @NonNull ObjectProvider<Site> siteProvider,
                      @Autowired @NonNull ContentRepository repository,
                      @Autowired @NonNull CapConnection connection) {
      this.siteProvider = siteProvider;
      this.repository = repository;
      structService = connection.getStructService();
    }

    @BeforeEach
    void setUp() {
      site = siteProvider.getObject();
    }

    @Test
    void shouldProvideEmptySourcesForUnavailableSettingsPath() {
      List<SettingsSource> settingsSources = SettingsSource.allAt(site, "settings");
      assertThat(settingsSources).isEmpty();
    }

    @Test
    void shouldProvideSingletonSourceForSettingsPathIsDocument() {
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

      List<SettingsSource> settingsSources = SettingsSource.allAt(site, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settingsSources)
        .hasSize(1)
        .allSatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key", "value")
        );
    }

    @Test
    void shouldProvideMultipleSourcesForSettingsPathIsFolder() {
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

      List<SettingsSource> settingsSources = SettingsSource.allAt(site, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settingsSources)
        .hasSize(2)
        .anySatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key1", "value1")
        )
        .anySatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key2", "value2")
        )
      ;
    }
  }

  @Nested
  class AllAtRepositoryBehavior {
    @NonNull
    private final ContentRepository repository;
    @NonNull
    private final StructService structService;

    AllAtRepositoryBehavior(@Autowired @NonNull ContentRepository repository,
                            @Autowired @NonNull CapConnection connection) {
      this.repository = repository;
      structService = connection.getStructService();
    }

    @Test
    void shouldProvideEmptySourcesForUnavailableSettingsPath() {
      List<SettingsSource> settingsSources = SettingsSource.allAt(repository, "settings");
      assertThat(settingsSources).isEmpty();
    }

    @Test
    void shouldProvideSingletonSourceForSettingsPathIsDocument() {
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

      List<SettingsSource> settingsSources = SettingsSource.allAt(repository, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settingsSources)
        .hasSize(1)
        .allSatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key", "value")
        );
    }

    @Test
    void shouldProvideMultipleSourcesForSettingsPathIsFolder() {
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

      List<SettingsSource> settingsSources = SettingsSource.allAt(repository, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settingsSources)
        .hasSize(2)
        .anySatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key1", "value1")
        )
        .anySatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key2", "value2")
        )
      ;
    }
  }

  @Nested
  class AllAtContentBehavior {
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
    void shouldProvideEmptySourcesForUnavailableSettingsPath() {
      List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings");
      assertThat(settingsSources).isEmpty();
    }

    @Test
    void shouldProvideSingletonSourceForSettingsPathIsDocument() {
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

      List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settingsSources)
        .hasSize(1)
        .allSatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key", "value")
        );
    }

    @Test
    void shouldProvideMultipleSourcesForSettingsPathIsFolder() {
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

      List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings", CT_SITE_CONTENT, "struct");

      assertThat(settingsSources)
        .hasSize(2)
        .anySatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key1", "value1")
        )
        .anySatisfy(
          source -> assertThat(source)
            .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
            .containsEntry("key2", "value2")
        )
      ;
    }

    /**
     * As content based API is the base for all other behaviors (at a site,
     * at the repository), we add some more tests (only) here regarding the
     * robustness behavior.
     */
    @Nested
    class RobustnessBehavior {
      @Test
      void shouldProvideEmptySourcesForEmptySettingsPath() {
        List<SettingsSource> settingsSources = SettingsSource.allAt(repository.getRoot(), "parent");
        assertThat(settingsSources).isEmpty();
      }

      @Test
      void shouldProvideSingletonSourceProvidingEmptyForUnavailableGlobalLinkSettings() {
        Struct settingsStruct = structService.emptyStruct();

        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .property("struct", settingsStruct)
          .checkedIn()
          .create();

        List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings", CT_SITE_CONTENT, "struct");

        assertThat(settingsSources)
          .hasSize(1)
          .allSatisfy(
            source -> assertThat(source)
              .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
              .isEmpty()
          );
      }

      @Test
      void shouldProvideSingletonSourceProvidingEmptyForUnavailableStructProperty() {
        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .checkedIn()
          .create();

        List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings", CT_SITE_CONTENT, "doesNotExist");

        assertThat(settingsSources)
          .hasSize(1)
          .allSatisfy(
            source -> assertThat(source)
              .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
              .isEmpty()
          );
      }

      @Test
      void shouldProvideSingletonSourceProvidingEmptyForNotStructProperty() {
        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .checkedIn()
          .create();

        List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings", CT_SITE_CONTENT, "string");

        assertThat(settingsSources)
          .hasSize(1)
          .allSatisfy(
            source -> assertThat(source)
              .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
              .isEmpty()
          );
      }

      @Test
      void shouldProvideSingletonSourceProvidingEmptyForUnmatchedContentType() {
        repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings")
          .checkedIn()
          .create();

        List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings", "SimpleEmpty", "irrelevant");

        assertThat(settingsSources)
          .hasSize(1)
          .allSatisfy(
            source -> assertThat(source)
              .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
              .isEmpty()
          );
      }

      @Test
      void shouldIgnoreMeanwhileDestroyDocumentInFolder() {
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

        Content destroyedContent = repository.createContentBuilder()
          .parent(parent)
          .type(CT_SITE_CONTENT)
          .name("settings/2")
          .property("struct", settingsStruct2)
          .checkedIn()
          .create();

        List<SettingsSource> settingsSources = SettingsSource.allAt(parent, "settings", CT_SITE_CONTENT, "struct");

        // Challenge our sources.
        destroyedContent.destroy();

        assertThat(settingsSources)
          .hasSize(2)
          .anySatisfy(
            source -> assertThat(source)
              .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
              .containsEntry("key1", "value1")
          )
          .anySatisfy(
            source -> assertThat(source)
              .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
              .isEmpty()
          )
          .noneSatisfy(
            source -> assertThat(source)
              .extracting(Supplier::get, InstanceOfAssertFactories.map(String.class, Object.class))
              .containsEntry("key2", "value2")
          )
        ;
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
