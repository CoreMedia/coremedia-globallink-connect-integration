package com.coremedia.labs.translation.gcc.util;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.assertj.core.api.InstanceOfAssertFactories;
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
class SettingsTest {
  @NonNull
  private final ApplicationContext context;

  SettingsTest(@Autowired @NonNull ApplicationContext context) {
    this.context = context;
  }

  @Nested
  class EmptyBehavior {
    @ParameterizedTest
    @EnumSource(EmptyFixture.class)
    void shouldHaveEmptyProperties(@NonNull EmptyFixture emptyFixture) {
      Settings settings = emptyFixture.get();
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEmpty();
    }

    enum EmptyFixture implements Supplier<Settings> {
      CONSTANT {
        @Override
        @NonNull
        public Settings get() {
          return Settings.EMPTY;
        }
      },
      OF_EMPTY_MAP {
        @Override
        @NonNull
        public Settings get() {
          return Settings.of(Map.of());
        }
      },
      BUILDER_WITHOUT_SOURCES {
        @Override
        @NonNull
        public Settings get() {
          return Settings.builder().build();
        }
      },
      BUILDER_WITH_EMPTY_SETTINGS_SOURCE {
        @Override
        @NonNull
        public Settings get() {
          return Settings.builder()
            .source(Settings.EMPTY)
            .build();
        }
      },
      BUILDER_WITH_EMPTY_MAP_SOURCE {
        @Override
        @NonNull
        public Settings get() {
          return Settings.builder()
            .source(Map::of)
            .build();
        }
      },
      BUILDER_WITH_EMPTY_SOURCES_LIST {
        @Override
        @NonNull
        public Settings get() {
          return Settings.builder()
            .sources(List.of())
            .build();
        }
      },
      BUILDER_WITH_EMPTY_SOURCES_ARRAY {
        @Override
        @NonNull
        public Settings get() {
          return Settings.builder()
            .sources()
            .build();
        }
      },
    }
  }

  @Nested
  @ParameterizedClass
  @EnumSource(SingleSourceBehavior.SingleSourceFixture.class)
  class SingleSourceBehavior {
    @NonNull
    private final SingleSourceFixture fixture;

    SingleSourceBehavior(@NonNull SingleSourceFixture fixture) {
      this.fixture = fixture;
    }

    @Test
    void shouldHaveExpectedProperties() {
      Map<String, Object> properties = Map.of("key", "value");
      Settings settings = fixture.apply(properties);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(properties);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 42})
    @ValueSource(classes = {Object.class, String.class})
    @ValueSource(booleans = {true, false})
    @ValueSource(longs = {Long.MIN_VALUE, Long.MAX_VALUE})
    @NullSource
    void shouldFilterOutMapsWithNonStringKeys(@Nullable Object key) {
      Map<Object, Object> nestedInvalidMap = new HashMap<>();
      nestedInvalidMap.put("valid_key", "value1");
      nestedInvalidMap.put(key, "value2");
      Map<String, Object> input = Map.of("containsInvalid", nestedInvalidMap);
      Map<String, Object> expected = Map.of("containsInvalid", Map.of("valid_key", "value1"));

      Settings settings = fixture.apply(input);

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldFilterEmptyValues(@NonNull EmptyValueFixture emptyValueFixture) {
      Map<String, Object> properties = new HashMap<>();
      properties.put("non-empty", "value");
      properties.put("empty", emptyValueFixture.get());
      Map<String, Object> expected = Map.of("non-empty", "value");
      Settings settings = fixture.apply(properties);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldFilterEmptyValuesDeeply(@NonNull EmptyValueFixture emptyValueFixture) {
      Map<String, Object> onlyEmpty = new HashMap<>();
      Map<String, Object> alsoEmpty = new HashMap<>();
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(properties);
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(properties);
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    enum SingleSourceFixture implements Function<Map<String, Object>, Settings> {
      OF {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> map) {
          return Settings.of(map);
        }
      },
      BUILDER_WITH_MAP_SOURCE {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> map) {
          return Settings.builder()
            .source(() -> map)
            .build();
        }
      },
      BUILDER_WITH_SETTINGS_SOURCE {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> map) {
          return Settings.builder()
            .source(Settings.of(map))
            .build();
        }
      },
      BUILDER_WITH_SOURCES_SINGLETON_LIST {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> map) {
          return Settings.builder()
            .sources(List.of(() -> map))
            .build();
        }
      },
      BUILDER_WITH_SOURCES_SINGLETON_ARRAY {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> map) {
          return Settings.builder()
            .sources(() -> map)
            .build();
        }
      },
    }
  }

  @ParameterizedClass
  @Nested
  @EnumSource(MultiSourceBehavior.MultiSource.class)
  class MultiSourceBehavior {
    @NonNull
    private final MultiSource fixture;

    MultiSourceBehavior(@NonNull MultiSource fixture) {
      this.fixture = fixture;
    }

    @Test
    void shouldMergeNonConflictingProperties() {
      Map<String, Object> first = Map.of("key1", "value1");
      Map<String, Object> second = Map.of("key2", "value2");
      Map<String, Object> expected = Map.of("key1", "value1", "key2", "value2");

      Settings settings = fixture.apply(first, second);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    @Test
    void shouldMergeNonConflictingPropertiesDeeply() {
      Map<String, Object> first = Map.of("parent", Map.of("key1", "value1"));
      Map<String, Object> second = Map.of("parent", Map.of("key2", "value2"));
      Map<String, Object> expected = Map.of("parent", Map.of("key1", "value1", "key2", "value2"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    @Test
    void shouldOverrideWithSecondSource() {
      Map<String, Object> first = Map.of("key1", "value1", "to-override", "original");
      Map<String, Object> second = Map.of("key2", "value2", "to-override", "overridden");
      Map<String, Object> expected = Map.of("key1", "value1", "key2", "value2", "to-override", "overridden");

      Settings settings = fixture.apply(first, second);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldNotOverrideWithSecondSourceEmptyValue(@NonNull EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = Map.of("key1", "value1", "to-override", "original");
      Map<String, Object> second = new HashMap<>(Map.of("key2", "value2"));
      second.put("to-override", emptyValueFixture.get());
      Map<String, Object> expected = Map.of("key1", "value1", "key2", "value2", "to-override", "original");

      Settings settings = fixture.apply(first, second);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    @Test
    void shouldOverrideDeeplyWithSecondSource() {
      Map<String, Object> first = Map.of("parent", Map.of("to-override", "original"));
      Map<String, Object> second = Map.of("parent", Map.of("to-override", "overridden"));
      Map<String, Object> expected = Map.of("parent", Map.of("to-override", "overridden"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldNotOverrideDeeplyWithSecondSourceEmptyValue(@NonNull EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = Map.of("parent", Map.of("to-override", "original"));
      Map<String, Object> emptyValue = new HashMap<>();
      emptyValue.put("to-override", emptyValueFixture.get());
      Map<String, Object> second = Map.of("parent", emptyValue);
      Map<String, Object> expected = Map.of("parent", Map.of("to-override", "original"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .isEqualTo(expected);
    }

    enum MultiSource implements BiFunction<Map<String, Object>, Map<String, Object>, Settings> {
      BUILDER_WITH_MAP_SOURCES {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> first, @NonNull Map<String, Object> second) {
          return Settings.builder()
            .source(() -> first)
            .source(() -> second)
            .build();
        }
      },
      BUILDER_WITH_SOURCES_LIST {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> first, @NonNull Map<String, Object> second) {
          return Settings.builder()
            .sources(List.of(() -> first, () -> second))
            .build();
        }
      },
      BUILDER_WITH_SOURCES_SINGLETON_ARRAY {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> first, @NonNull Map<String, Object> second) {
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

      assertThat(settings)
        .extracting(Settings::properties, InstanceOfAssertFactories.map(String.class, Object.class))
        .containsExactly(Map.entry("source", "context"));
    }
  }

  @Nested
  @ParameterizedClass
  @EnumSource(AtBehavior.AtVariant.class)
  class AtBehavior {
    @NonNull
    private final AtVariant atVariant;

    AtBehavior(@NonNull AtVariant atVariant) {
      this.atVariant = atVariant;
    }

    @ParameterizedTest
    @EnumSource(SettingsFixture.class)
    void shouldReturnEmptyIfNotFoundInDirectProperties(@NonNull SettingsFixture settingsFixture) {
      Settings settings = settingsFixture.get();
      Optional<Object> result = atVariant.apply(settings, List.of("unavailable"));
      assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(SettingsFixture.class)
    void shouldReturnEmptyIfNotFoundInNestedProperties(@NonNull SettingsFixture settingsFixture) {
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
      EMPTY {
        @Override
        public Settings get() {
          return Settings.EMPTY;
        }
      },
      SINGLETON_ENTRY {
        @Override
        public Settings get() {
          return Settings.of(Map.of("key", "value"));
        }
      },
    }

    enum AtVariant implements BiFunction<Settings, List<String>, Optional<Object>> {
      ARRAY_PARAMETER {
        @Override
        public Optional<Object> apply(Settings settings, List<String> strings) {
          String first = strings.stream().findFirst().orElseThrow();
          String[] others = strings.stream().skip(1).toArray(String[]::new);
          return settings.at(first, others);
        }
      },
      LIST_PARAMETER {
        @Override
        public Optional<Object> apply(Settings settings, List<String> strings) {
          return settings.at(strings);
        }
      },
    }
  }

  enum EmptyValueFixture implements Supplier<Object> {
    NULL {
      @Override
      public Object get() {
        return null;
      }
    },
    EMPTY_STRING {
      @Override
      public Object get() {
        return "";
      }
    },
    EMPTY_SET {
      @Override
      public Object get() {
        return Set.of();
      }
    },
    EMPTY_LIST {
      @Override
      public Object get() {
        return List.of();
      }
    },
    EMPTY_MAP {
      @Override
      public Object get() {
        return Map.of();
      }
    },
  }

  @Configuration(proxyBeanMethods = false)
  static class LocalConfig {
    @Bean
    public Map<String, Object> gccConfigurationProperties() {
      return Map.of("source", "context");
    }
  }
}
