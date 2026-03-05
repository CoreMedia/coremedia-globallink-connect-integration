package com.coremedia.labs.translation.gcc.util;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@NullMarked
class SettingsTest {
  @Nested
  class EmptyBehavior {
    @ParameterizedTest
    @EnumSource(EmptyFixture.class)
    void shouldHaveEmptyProperties(EmptyFixture emptyFixture) {
      Settings settings = emptyFixture.get();
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEmpty(),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    /**
     * Represents various ways to achieve an empty state.
     */
    enum EmptyFixture implements Supplier<Settings> {
      CONSTANT(Settings.EMPTY),
      FROM_EMPTY_MAP(new Settings(Map.of())),
      FROM_EMPTY_MUTABLE_MAP(new Settings(new HashMap<>()));

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
      Map<String, @Nullable Object> expectedProperties = Map.of("key", "value");
      Settings settings = fixture.apply(new HashMap<>(expectedProperties));
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @SuppressWarnings("JUnitMalformedDeclaration") // false-positive for @NullSource
    @ParameterizedTest
    @ValueSource(ints = {0, 42})
    @ValueSource(classes = {Object.class, String.class})
    @ValueSource(booleans = {true, false})
    @ValueSource(longs = {Long.MIN_VALUE, Long.MAX_VALUE})
    @NullSource
    void shouldFilterOutMapsWithNonStringKeys(@Nullable Object key) {
      Map<@Nullable Object, @Nullable Object> nestedInvalidMap = new HashMap<>();
      nestedInvalidMap.put("valid_key", "value1");
      nestedInvalidMap.put(key, "value2");
      Map<String, @Nullable Object> input = new HashMap<>(Map.of("containsInvalid", nestedInvalidMap));
      Map<String, Object> expectedProperties = Map.of("containsInvalid", Map.of("valid_key", "value1"));

      Settings settings = fixture.apply(input);

      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldFilterEmptyValues(EmptyValueFixture emptyValueFixture) {
      Map<String, @Nullable Object> input = new HashMap<>();
      input.put("non-empty", "value");
      input.put("empty", emptyValueFixture.get());
      Map<String, Object> expectedProperties = Map.of("non-empty", "value");
      Settings settings = fixture.apply(input);
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldFilterEmptyValuesDeeply(EmptyValueFixture emptyValueFixture) {
      Map<String, @Nullable Object> onlyEmpty = new HashMap<>();
      Map<String, @Nullable Object> alsoEmpty = new HashMap<>();
      onlyEmpty.put("empty", emptyValueFixture.get());
      alsoEmpty.put("empty", emptyValueFixture.get());
      alsoEmpty.put("non-empty", "value");

      Map<String, @Nullable Object> input = Map.of(
        "non-empty", "value",
        "parent-only-empty", onlyEmpty,
        "parent-also-empty", alsoEmpty
      );

      Map<String, Object> expectedProperties = Map.of(
        "non-empty", "value",
        "parent-also-empty", Map.of(
          "non-empty", "value"
        )
      );

      Settings settings = fixture.apply(input);

      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @Test
    void shouldRespectMapEntriesUpToMaxDepth() {
      Map<String, @Nullable Object> input = new HashMap<>();
      Map<String, @Nullable Object> current = input;
      current.put("level", 0);
      for (int i = 0; i < Settings.MAX_DEPTH; i++) {
        Map<String, @Nullable Object> next = new HashMap<>();
        next.put("level", i + 1);
        current.put("next", next);
        current = next;
      }
      Map<String, Object> expectedProperties = ImmutableMap.copyOf(input);

      Settings settings = fixture.apply(input);

      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
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

      Map<String, @Nullable Object> input = new HashMap<>(Map.of("list", list));
      Map<String, Object> expectedProperties = ImmutableMap.copyOf(input);

      Settings settings = fixture.apply(input);

      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @Test
    void shouldStripMapEntriesBelowMaxDepth() {
      Map<String, @Nullable Object> input = new HashMap<>();
      Map<String, @Nullable Object> current = input;
      Map<String, Object> expectedProperties = new HashMap<>();
      Map<String, Object> currentExpected = expectedProperties;

      current.put("level", 0);
      currentExpected.put("level", 0);

      for (int i = 0; i < Settings.MAX_DEPTH + 1; i++) {
        Map<String, @Nullable Object> next = new HashMap<>();
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

      Settings settings = fixture.apply(input);

      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
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

      Map<String, @Nullable Object> properties = Map.of("list", list);
      Map<String, Object> expected = Map.of("list", expectedList);

      Settings settings = fixture.apply(properties);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    /**
     * Represents different strategies for applying sources.
     */
    enum SingleSourceFixture implements Function<Map<String, @Nullable Object>, Settings> {
      CONSTRUCTOR() {
        @Override
        public Settings apply(Map<String, @Nullable Object> map) {
          return Settings.ofSanitized(map);
        }
      },
      EMPTY_PUT_ALL_SETTINGS() {
        @Override
        public Settings apply(Map<String, @Nullable Object> map) {
          return Settings.EMPTY.mergedWith(Settings.ofSanitized(map));
        }
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
      Map<String, Object> first = new HashMap<>(Map.of("key1", "value1"));
      Map<String, Object> second = new HashMap<>(Map.of("key2", "value2"));
      Map<String, Object> expectedProperties = Map.of("key1", "value1", "key2", "value2");

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @Test
    void shouldMergeNonConflictingPropertiesDeeply() {
      Map<String, Object> first = new HashMap<>(Map.of("parent", new HashMap<>(Map.of("key1", "value1"))));
      Map<String, Object> second = new HashMap<>(Map.of("parent", new HashMap<>(Map.of("key2", "value2"))));
      Map<String, Object> expectedProperties = Map.of("parent", Map.of("key1", "value1", "key2", "value2"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @Test
    void shouldOverrideWithSecondSource() {
      Map<String, Object> first = new HashMap<>(Map.of("key1", "value1", "to-override", "original"));
      Map<String, Object> second = new HashMap<>(Map.of("key2", "value2", "to-override", "overridden"));
      Map<String, Object> expectedProperties = Map.of("key1", "value1", "key2", "value2", "to-override", "overridden");

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldNotOverrideWithSecondSourceEmptyValue(EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = new HashMap<>(Map.of("key1", "value1", "to-override", "original"));
      Map<String, @Nullable Object> second = new HashMap<>(Map.of("key2", "value2"));
      second.put("to-override", emptyValueFixture.get());
      Map<String, Object> expectedProperties = Map.of("key1", "value1", "key2", "value2", "to-override", "original");

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @Test
    void shouldOverrideDeeplyWithSecondSource() {
      Map<String, Object> first = new HashMap<>(Map.of("parent", new HashMap<>(Map.of("to-override", "original"))));
      Map<String, Object> second = new HashMap<>(Map.of("parent", new HashMap<>(Map.of("to-override", "overridden"))));
      Map<String, Object> expectedProperties = Map.of("parent", Map.of("to-override", "overridden"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldNotOverrideDeeplyWithSecondSourceEmptyValue(EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = new HashMap<>(Map.of("parent", new HashMap<>(Map.of("to-override", "original"))));
      Map<String, @Nullable Object> emptyValue = new HashMap<>();
      emptyValue.put("to-override", emptyValueFixture.get());
      Map<String, Object> second = Map.of("parent", emptyValue);
      Map<String, Object> expectedProperties = Map.of("parent", Map.of("to-override", "original"));

      Settings settings = fixture.apply(first, second);
      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @Test
    void shouldRespectEntriesUpToMaxDepth() {
      Map<String, Object> first = new HashMap<>(Map.of("key1", "value1"));
      Map<String, Object> expectedProperties = new HashMap<>();
      Map<String, Object> second = new HashMap<>();
      Map<String, Object> current = second;
      Map<String, Object> currentExpected = expectedProperties;
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
      expectedProperties.putAll(first);

      Settings settings = fixture.apply(first, second);

      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    @Test
    void shouldStripEntriesBelowMaxDepth() {
      Map<String, Object> first = Map.of("key1", "value1");
      Map<String, Object> second = new HashMap<>();
      Map<String, Object> current = second;
      Map<String, Object> expectedProperties = new HashMap<>();
      Map<String, Object> currentExpected = expectedProperties;

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
      expectedProperties.putAll(first);

      Settings settings = fixture.apply(first, second);

      assertThat(settings.properties())
        .satisfies(
          properties -> assertThat(properties).isEqualTo(expectedProperties),
          SettingsTest::assertIsDeeplyUnmodifiable
        );
    }

    /**
     * Represents different strategies for applying multiple sources.
     */
    enum MultiSource implements BiFunction<Map<String, @Nullable Object>, Map<String, @Nullable Object>, Settings> {
      PUT_ALL_SETTINGS {
        @Override
        public Settings apply(Map<String, @Nullable Object> first, Map<String, @Nullable Object> second) {
          return Settings.ofSanitized(first).mergedWith(Settings.ofSanitized(second));
        }
      },
      USING_COLLECTOR {
        @Override
        public Settings apply(Map<String, @Nullable Object> first, Map<String, @Nullable Object> second) {
          return Stream.of(first, second)
            .map(Settings::new)
            .reduce(Settings::mergedWith)
            .orElse(Settings.EMPTY);
        }
      }
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
      Settings settings = new Settings(Map.of("key", "value"));
      Optional<Object> result = atVariant.apply(settings, List.of("key"));
      assertThat(result).hasValue("value");
    }

    @Test
    void shouldReturnExistingNestedProperty() {
      Settings settings = new Settings(Map.of("parent", Map.of("key", "value")));
      Optional<Object> result = atVariant.apply(settings, List.of("parent", "key"));
      assertThat(result).hasValue("value");
    }

    @Test
    void shouldReturnExistingDeeperNestedProperty() {
      Settings settings = new Settings(Map.of("grandparent", Map.of("parent", Map.of("key", "value"))));
      Optional<Object> result = atVariant.apply(settings, List.of("grandparent", "parent", "key"));
      assertThat(result).hasValue("value");
    }

    enum SettingsFixture implements Supplier<Settings> {
      EMPTY(Settings.EMPTY),
      SINGLETON_ENTRY(new Settings(Map.of("key", "value")));

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
          String[] others = strings.stream().skip(1L).toArray(String[]::new);
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

  private static void assertIsDeeplyUnmodifiable(Map<String, Object> map) {
    assertThatThrownBy(() -> map.put("key", "value")).isInstanceOf(UnsupportedOperationException.class);
    map.values().forEach(value -> {
      if (value instanceof Map<?, ?> nestedMap) {
        @SuppressWarnings("unchecked") Map<String, Object> castedMap = (Map<String, Object>) nestedMap;
        assertIsDeeplyUnmodifiable(castedMap);
      } else if (value instanceof Collection<?> nestedList) {
        @SuppressWarnings("unchecked") Collection<Object> castedList = (Collection<Object>) nestedList;
        assertIsDeeplyUnmodifiable(castedList);
      }
    });
  }

  private static void assertIsDeeplyUnmodifiable(Collection<Object> list) {
    assertThatThrownBy(() -> list.add("value")).isInstanceOf(UnsupportedOperationException.class);
    list.forEach(value -> {
      if (value instanceof Map<?, ?> nestedMap) {
        @SuppressWarnings("unchecked") Map<String, Object> castedMap = (Map<String, Object>) nestedMap;
        assertIsDeeplyUnmodifiable(castedMap);
      } else if (value instanceof Collection<?> nestedList) {
        @SuppressWarnings("unchecked") Collection<Object> castedList = (Collection<Object>) nestedList;
        assertIsDeeplyUnmodifiable(castedList);
      }
    });
  }

  /**
   * Some representations of empty values that shall be ignored for the
   * resulting properties.
   */
  enum EmptyValueFixture implements Supplier<@Nullable Object> {
    NULL() {
      @Override
      public @Nullable Object get() {
        return null;
      }
    },
    EMPTY_STRING() {
      @Override
      public Object get() {
        return "";
      }
    },
    EMPTY_SET() {
      @Override
      public Object get() {
        return Set.of();
      }
    },
    EMPTY_MUTABLE_SET() {
      @Override
      public Object get() {
        return new HashSet<>();
      }
    },
    EMPTY_LIST() {
      @Override
      public Object get() {
        return List.of();
      }
    },
    EMPTY_MUTABLE_LIST() {
      @Override
      public Object get() {
        return new ArrayList<>();
      }
    },
    EMPTY_MAP() {
      @Override
      public Object get() {
        return Map.of();
      }
    },
    EMPTY_MUTABLE_MAP() {
      @Override
      public Object get() {
        return new HashMap<>();
      }
    }
  }
}
