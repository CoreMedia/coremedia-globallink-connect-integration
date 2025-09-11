package com.coremedia.labs.translation.gcc.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(SettingsTest.LocalConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@NullMarked
class SettingsTest {
  private final ApplicationContext context;

  SettingsTest(@Autowired ApplicationContext context) {
    this.context = context;
  }

  @Nested
  class EmptyBehavior {
    @ParameterizedTest
    @EnumSource(EmptyFixture.class)
    void shouldHaveEmptyProperties(EmptyFixture emptyFixture) {
      Settings settings = emptyFixture.get();
      assertThat(settings.properties()).isEmpty();
    }

    /**
     * Represents various ways to achieve an empty state.
     */
    enum EmptyFixture implements Supplier<Settings> {
      CONSTANT(Settings.EMPTY),
      OF_EMPTY_MAP(Settings.of(Map.of())),
      BUILDER_WITHOUT_SOURCES(Settings.builder().build()),
      BUILDER_WITH_EMPTY_SETTINGS_SOURCE(Settings.builder()
        .source(Settings.EMPTY)
        .build()),
      BUILDER_WITH_EMPTY_MAP_SOURCE(Settings.builder()
        .source(Map::of)
        .build()),
      BUILDER_WITH_EMPTY_SOURCES_LIST(Settings.builder()
        .sources(List.of())
        .build()),
      BUILDER_WITH_EMPTY_SOURCES_ARRAY(Settings.builder()
        .sources()
        .build());

      private final Settings settings;

      EmptyFixture(Settings settings) {
        this.settings = settings;
      }

      @Override
      public Settings get() {
        return settings;
      }
    }
  }

  /**
   * Test for only a single source to be added, thus, merging does not play
   * a role here.
   */
  @Nested
  @ParameterizedClass
  @EnumSource(SingleSourceBehavior.SingleSourceFixture.class)
  class SingleSourceBehavior {
    private final SingleSourceFixture fixture;

    SingleSourceBehavior(SingleSourceFixture fixture) {
      this.fixture = fixture;
    }

    @Test
    void shouldHaveExpectedProperties() {
      Map<String, Object> properties = Map.of("key", "value");
      Settings settings = fixture.apply(properties);
      assertThat(settings.properties()).isEqualTo(properties);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 42})
    @ValueSource(classes = {Object.class, String.class})
    @ValueSource(booleans = {true, false})
    @ValueSource(longs = {Long.MIN_VALUE, Long.MAX_VALUE})
    @NullSource
    void shouldFilterOutMapsWithNonStringKeys(@Nullable Object key) {
      Map<@Nullable Object, Object> nestedInvalidMap = new HashMap<>();
      nestedInvalidMap.put("valid_key", "value1");
      nestedInvalidMap.put(key, "value2");
      Map<String, Object> input = Map.of("containsInvalid", nestedInvalidMap);
      Map<String, Object> expected = Map.of("containsInvalid", Map.of("valid_key", "value1"));

      Settings settings = fixture.apply(input);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldFilterEmptyValues(EmptyValueFixture emptyValueFixture) {
      Map<String, @Nullable Object> properties = new HashMap<>();
      properties.put("non-empty", "value");
      properties.put("empty", emptyValueFixture.get());
      Map<String, Object> expected = Map.of("non-empty", "value");
      Settings settings = fixture.apply(properties);
      assertThat(settings.properties()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldFilterEmptyValuesDeeply(EmptyValueFixture emptyValueFixture) {
      Map<String, @Nullable Object> onlyEmpty = new HashMap<>();
      Map<String, @Nullable Object> alsoEmpty = new HashMap<>();
      onlyEmpty.put("empty", emptyValueFixture.get());
      alsoEmpty.put("empty", emptyValueFixture.get());
      alsoEmpty.put("non-empty", "value");

      Map<String, Object> properties = Map.of(
        "non-empty", "value",
        "parent-only-empty", onlyEmpty,
        "parent-also-empty", alsoEmpty
      );

      Map<String, Object> expected = Map.of(
        "non-empty", "value",
        "parent-also-empty", Map.of(
          "non-empty", "value"
        )
      );

      Settings settings = fixture.apply(properties);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    @Test
    void shouldRespectMapEntriesUpToMaxDepth() {
      Map<String, Object> properties = new HashMap<>();
      Map<String, Object> current = properties;
      current.put("level", 0);
      for (int i = 0; i < Settings.MAX_DEPTH; i++) {
        Map<String, Object> next = new HashMap<>();
        next.put("level", i + 1);
        current.put("next", next);
        current = next;
      }

      Settings settings = fixture.apply(properties);

      assertThat(settings.properties()).isEqualTo(properties);
    }

    @Test
    void shouldRespectListEntriesUpToMaxDepth() {
      List<Object> list = new ArrayList<>();
      List<Object> currentList = list;

      currentList.add("level: %d".formatted(0));

      // One less than max, as the map is already level 0
      for (int i = 0; i < Settings.MAX_DEPTH - 1; i++) {
        List<Object> next = new ArrayList<>();
        next.add("level: %d".formatted(i + 1));
        currentList.add(next);
        currentList = next;
      }

      Map<String, Object> properties = Map.of("list", list);

      Settings settings = fixture.apply(properties);

      assertThat(settings.properties()).isEqualTo(properties);
    }

    @Test
    void shouldStripMapEntriesBelowMaxDepth() {
      Map<String, Object> properties = new HashMap<>();
      Map<String, Object> current = properties;
      Map<String, Object> expected = new HashMap<>();
      Map<String, Object> currentExpected = expected;

      current.put("level", 0);
      currentExpected.put("level", 0);

      for (int i = 0; i < Settings.MAX_DEPTH + 1; i++) {
        Map<String, Object> next = new HashMap<>();
        Map<String, Object> nextExpected = new HashMap<>();
        next.put("level", i + 1);
        nextExpected.put("level", i + 1);
        current.put("next", next);
        if (i < Settings.MAX_DEPTH) {
          currentExpected.put("next", nextExpected);
        }
        current = next;
        currentExpected = nextExpected;
      }

      Settings settings = fixture.apply(properties);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    @Test
    void shouldStripListEntriesBelowMaxDepth() {
      List<Object> list = new ArrayList<>();
      List<Object> expectedList = new ArrayList<>();
      List<Object> currentList = list;
      List<Object> currentExpectedList = expectedList;

      currentList.add("level: %d".formatted(0));
      currentExpectedList.add("level: %d".formatted(0));

      for (int i = 0; i < Settings.MAX_DEPTH; i++) {
        List<Object> next = new ArrayList<>();
        List<Object> nextExpected = new ArrayList<>();
        next.add("level: %d".formatted(i + 1));
        nextExpected.add("level: %d".formatted(i + 1));
        currentList.add(next);
        if (i < Settings.MAX_DEPTH - 1) {
          currentExpectedList.add(nextExpected);
        }
        currentList = next;
        currentExpectedList = nextExpected;
      }

      Map<String, Object> properties = Map.of("list", list);
      Map<String, Object> expected = Map.of("list", expectedList);

      Settings settings = fixture.apply(properties);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    /**
     * Represents different strategies for applying sources:
     * <ul>
     * <li>{@link Settings#of(Map)}</li>
     * <li>{@link Settings.Builder#source(SettingsSource)}</li>
     * <li>{@link Settings.Builder#source(Settings)}</li>
     * <li>{@link Settings.Builder#sources(List)}</li>
     * <li>{@link Settings.Builder#sources(SettingsSource...)}</li>
     * </ul>
     */
    enum SingleSourceFixture implements Function<Map<String, @Nullable Object>, Settings> {
      OF(Settings::of),
      BUILDER_WITH_MAP_SOURCE(map -> Settings.builder()
        .source(() -> map)
        .build()),
      BUILDER_WITH_SETTINGS_SOURCE(map -> Settings.builder()
        .source(Settings.of(map))
        .build()),
      BUILDER_WITH_SOURCES_SINGLETON_LIST(map -> Settings.builder()
        .sources(List.of(() -> map))
        .build()),
      BUILDER_WITH_SOURCES_SINGLETON_ARRAY(map -> Settings.builder()
        .sources(() -> map)
        .build());

      private final Function<Map<String, @Nullable Object>, Settings> delegate;

      SingleSourceFixture(Function<Map<String, @Nullable Object>, Settings> delegate) {
        this.delegate = delegate;
      }

      @Override
      public Settings apply(Map<String, @Nullable Object> map) {
        return delegate.apply(map);
      }
    }
  }

  /**
   * Tests merging multiple sources and respecting their order.
   */
  @ParameterizedClass
  @Nested
  @EnumSource(MultiSourceBehavior.MultiSource.class)
  class MultiSourceBehavior {
    private final MultiSource fixture;

    MultiSourceBehavior(MultiSource fixture) {
      this.fixture = fixture;
    }

    @Test
    void shouldMergeNonConflictingProperties() {
      Map<String, Object> first = Map.of("key1", "value1");
      Map<String, Object> second = Map.of("key2", "value2");
      Map<String, Object> expected = Map.of("key1", "value1", "key2", "value2");

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties()).isEqualTo(expected);
    }

    @Test
    void shouldMergeNonConflictingPropertiesDeeply() {
      Map<String, Object> first = Map.of("parent", Map.of("key1", "value1"));
      Map<String, Object> second = Map.of("parent", Map.of("key2", "value2"));
      Map<String, Object> expected = Map.of("parent", Map.of("key1", "value1", "key2", "value2"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties()).isEqualTo(expected);
    }

    @Test
    void shouldOverrideWithSecondSource() {
      Map<String, Object> first = Map.of("key1", "value1", "to-override", "original");
      Map<String, Object> second = Map.of("key2", "value2", "to-override", "overridden");
      Map<String, Object> expected = Map.of("key1", "value1", "key2", "value2", "to-override", "overridden");

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldNotOverrideWithSecondSourceEmptyValue(EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = Map.of("key1", "value1", "to-override", "original");
      Map<String, @Nullable Object> second = new HashMap<>(Map.of("key2", "value2"));
      second.put("to-override", emptyValueFixture.get());
      Map<String, Object> expected = Map.of("key1", "value1", "key2", "value2", "to-override", "original");

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties()).isEqualTo(expected);
    }

    @Test
    void shouldOverrideDeeplyWithSecondSource() {
      Map<String, Object> first = Map.of("parent", Map.of("to-override", "original"));
      Map<String, Object> second = Map.of("parent", Map.of("to-override", "overridden"));
      Map<String, Object> expected = Map.of("parent", Map.of("to-override", "overridden"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldNotOverrideDeeplyWithSecondSourceEmptyValue(EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = Map.of("parent", Map.of("to-override", "original"));
      Map<String, @Nullable Object> emptyValue = new HashMap<>();
      emptyValue.put("to-override", emptyValueFixture.get());
      Map<String, Object> second = Map.of("parent", emptyValue);
      Map<String, Object> expected = Map.of("parent", Map.of("to-override", "original"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties()).isEqualTo(expected);
    }

    @Test
    void shouldRespectEntriesUpToMaxDepth() {
      Map<String, Object> first = Map.of("key1", "value1");
      Map<String, Object> expected = new HashMap<>();
      Map<String, Object> second = new HashMap<>();
      Map<String, Object> current = second;
      Map<String, Object> currentExpected = expected;
      current.put("level", 0);
      currentExpected.put("level", 0);
      for (int i = 0; i < Settings.MAX_DEPTH; i++) {
        Map<String, Object> next = new HashMap<>();
        Map<String, Object> nextExpected = new HashMap<>();
        next.put("level", i + 1);
        nextExpected.put("level", i + 1);
        current.put("next", next);
        currentExpected.put("next", nextExpected);
        current = next;
        currentExpected = nextExpected;
      }
      expected.putAll(first);

      Settings settings = fixture.apply(first, second);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    @Test
    void shouldStripEntriesBelowMaxDepth() {
      Map<String, Object> first = Map.of("key1", "value1");
      Map<String, Object> second = new HashMap<>();
      Map<String, Object> current = second;
      Map<String, Object> expected = new HashMap<>();
      Map<String, Object> currentExpected = expected;

      current.put("level", 0);
      currentExpected.put("level", 0);

      for (int i = 0; i < Settings.MAX_DEPTH + 1; i++) {
        Map<String, Object> next = new HashMap<>();
        Map<String, Object> nextExpected = new HashMap<>();
        next.put("level", i + 1);
        nextExpected.put("level", i + 1);
        current.put("next", next);
        if (i < Settings.MAX_DEPTH) {
          currentExpected.put("next", nextExpected);
        }
        current = next;
        currentExpected = nextExpected;
      }
      expected.putAll(first);

      Settings settings = fixture.apply(first, second);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    /**
     * Represents different strategies for applying multiple sources:
     * <ul>
     * <li>{@link Settings.Builder#source(SettingsSource)}</li>
     * <li>{@link Settings.Builder#sources(List)}</li>
     * <li>{@link Settings.Builder#sources(SettingsSource...)}</li>
     * </ul>
     */
    enum MultiSource implements BiFunction<Map<String, @Nullable Object>, Map<String, @Nullable Object>, Settings> {
      BUILDER_WITH_MAP_SOURCES {
        @Override
        public Settings apply(Map<String, @Nullable Object> first, Map<String, @Nullable Object> second) {
          return Settings.builder()
            .source(() -> first)
            .source(() -> second)
            .build();
        }
      },
      BUILDER_WITH_SOURCES_LIST {
        @Override
        public Settings apply(Map<String, @Nullable Object> first, Map<String, @Nullable Object> second) {
          return Settings.builder()
            .sources(List.of(() -> first, () -> second))
            .build();
        }
      },
      BUILDER_WITH_SOURCES_SINGLETON_ARRAY {
        @Override
        public Settings apply(Map<String, @Nullable Object> first, Map<String, @Nullable Object> second) {
          return Settings.builder()
            .sources(() -> first, () -> second)
            .build();
        }
      },
    }
  }

  /**
   * Just testing the context source. This is because the other convenience
   * methods require the {@code CMSettings} Blueprint content-type, that is
   * unavailable in this test scenario. Expectation is, that this behavior
   * is sufficiently covered by tests for {@link SettingsSource}.
   */
  @Nested
  class ConvenienceSourceBehavior {
    @Test
    void shouldAddPropertiesFromContext() {
      Settings settings = Settings.builder().beanSource(context).build();

      assertThat(settings.properties())
        .containsExactly(Map.entry("source", "context"));
    }
  }

  /**
   * Tests {@link Settings#at(List)} and {@link Settings#at(String, String...)}.
   */
  @Nested
  @ParameterizedClass
  @EnumSource(AtBehavior.AtVariant.class)
  class AtBehavior {
    private final AtVariant atVariant;

    AtBehavior(AtVariant atVariant) {
      this.atVariant = atVariant;
    }

    @ParameterizedTest
    @EnumSource(SettingsFixture.class)
    void shouldReturnEmptyIfNotFoundInDirectProperties(SettingsFixture settingsFixture) {
      Settings settings = settingsFixture.get();
      Optional<Object> result = atVariant.apply(settings, List.of("unavailable"));
      assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(SettingsFixture.class)
    void shouldReturnEmptyIfNotFoundInNestedProperties(SettingsFixture settingsFixture) {
      Settings settings = settingsFixture.get();
      Optional<Object> result = atVariant.apply(settings, List.of("key", "unavailable"));
      assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnExistingDirectProperty() {
      Settings settings = Settings.of(Map.of("key", "value"));
      Optional<Object> result = atVariant.apply(settings, List.of("key"));
      assertThat(result).hasValue("value");
    }

    @Test
    void shouldReturnExistingNestedProperty() {
      Settings settings = Settings.of(Map.of("parent", Map.of("key", "value")));
      Optional<Object> result = atVariant.apply(settings, List.of("parent", "key"));
      assertThat(result).hasValue("value");
    }

    @Test
    void shouldReturnExistingDeeperNestedProperty() {
      Settings settings = Settings.of(Map.of("grandparent", Map.of("parent", Map.of("key", "value"))));
      Optional<Object> result = atVariant.apply(settings, List.of("grandparent", "parent", "key"));
      assertThat(result).hasValue("value");
    }

    enum SettingsFixture implements Supplier<Settings> {
      EMPTY(Settings.EMPTY),
      SINGLETON_ENTRY(Settings.of(Map.of("key", "value")));

      private final Settings settings;

      SettingsFixture(Settings settings) {
        this.settings = settings;
      }

      @Override
      public Settings get() {
        return settings;
      }
    }

    /**
     * Use different implementations of {@code Settings.at(...)}.
     */
    enum AtVariant implements BiFunction<Settings, List<String>, Optional<Object>> {
      /**
       * Strategy using {@link Settings#at(String, String...)}.
       */
      ARRAY_PARAMETER {
        @Override
        public Optional<Object> apply(Settings settings, List<String> strings) {
          String first = strings.stream().findFirst().orElseThrow();
          String[] others = strings.stream().skip(1).toArray(String[]::new);
          return settings.at(first, others);
        }
      },
      /**
       * Strategy using {@link Settings#at(List)}
       */
      LIST_PARAMETER {
        @Override
        public Optional<Object> apply(Settings settings, List<String> strings) {
          return settings.at(strings);
        }
      },
    }
  }

  /**
   * Some representations of empty values that shall be ignored for the
   * resulting properties.
   */
  enum EmptyValueFixture implements Supplier<@Nullable Object> {
    NULL(null),
    EMPTY_STRING(""),
    EMPTY_SET(Set.of()),
    EMPTY_LIST(List.of()),
    EMPTY_MAP(Map.of());

    @Nullable
    private final Object value;

    EmptyValueFixture(@Nullable Object value) {
      this.value = value;
    }

    @Override
    @Nullable
    public Object get() {
      return value;
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class LocalConfig {
    @Bean
    public Map<String, Object> gccConfigurationProperties() {
      return Map.of("source", "context");
    }
  }
}
