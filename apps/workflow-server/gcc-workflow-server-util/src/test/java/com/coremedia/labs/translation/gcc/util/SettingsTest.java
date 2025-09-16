package com.coremedia.labs.translation.gcc.util;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsTest {
  @Nested
  class EmptyBehavior {
    @ParameterizedTest
    @EnumSource(EmptyFixture.class)
    void shouldHaveEmptyProperties(@NonNull EmptyFixture emptyFixture) {
      Settings settings = emptyFixture.get();
      assertThat(settings.properties()).isEmpty();
    }

    /**
     * Represents various ways to achieve an empty state.
     */
    enum EmptyFixture implements Supplier<Settings> {
      CONSTANT(Settings.EMPTY),
      FROM_EMPTY_MAP(new Settings(Map.of()));

      @NonNull
      private final Settings settings;

      EmptyFixture(@NonNull Settings settings) {
        this.settings = settings;
      }

      @Override
      @NonNull
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
    @NonNull
    private final SingleSourceFixture fixture;

    SingleSourceBehavior(@NonNull SingleSourceFixture fixture) {
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
      Map<Object, Object> nestedInvalidMap = new HashMap<>();
      nestedInvalidMap.put("valid_key", "value1");
      nestedInvalidMap.put(key, "value2");
      Map<String, Object> input = Map.of("containsInvalid", nestedInvalidMap);
      Map<String, Object> expected = Map.of("containsInvalid", Map.of("valid_key", "value1"));

      Settings settings = fixture.apply(input);

      assertThat(settings.properties()).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(EmptyValueFixture.class)
    void shouldFilterEmptyValues(@NonNull EmptyValueFixture emptyValueFixture) {
      Map<String, Object> properties = new HashMap<>();
      properties.put("non-empty", "value");
      properties.put("empty", emptyValueFixture.get());
      Map<String, Object> expected = Map.of("non-empty", "value");
      Settings settings = fixture.apply(properties);
      assertThat(settings.properties()).isEqualTo(expected);
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
     * Represents different strategies for applying sources.
     */
    enum SingleSourceFixture implements Function<Map<String, Object>, Settings> {
      CONSTRUCTOR(Settings::new),
      EMPTY_PUT_ALL_SETTINGS(other -> Settings.EMPTY.putAll(new Settings(other)));

      @NonNull
      private final Function<Map<String, Object>, Settings> delegate;

      SingleSourceFixture(@NonNull Function<Map<String, Object>, Settings> delegate) {
        this.delegate = delegate;
      }

      @Override
      @NonNull
      public Settings apply(@NonNull Map<String, Object> map) {
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
    void shouldNotOverrideWithSecondSourceEmptyValue(@NonNull EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = Map.of("key1", "value1", "to-override", "original");
      Map<String, Object> second = new HashMap<>(Map.of("key2", "value2"));
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
    void shouldNotOverrideDeeplyWithSecondSourceEmptyValue(@NonNull EmptyValueFixture emptyValueFixture) {
      Map<String, Object> first = Map.of("parent", Map.of("to-override", "original"));
      Map<String, Object> emptyValue = new HashMap<>();
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
     * Represents different strategies for applying multiple sources.
     */
    enum MultiSource implements BiFunction<Map<String, Object>, Map<String, Object>, Settings> {
      PUT_ALL_SETTINGS {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> first, @NonNull Map<String, Object> second) {
          return new Settings(first).putAll(new Settings(second));
        }
      },
      USING_COLLECTOR {
        @Override
        @NonNull
        public Settings apply(@NonNull Map<String, Object> first, @NonNull Map<String, Object> second) {
          return Stream.of(first, second)
            .map(Settings::new)
            .reduce(Settings::putAll)
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

      @NonNull
      private final Settings settings;

      SettingsFixture(@NonNull Settings settings) {
        this.settings = settings;
      }

      @Override
      @NonNull
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
  enum EmptyValueFixture implements Supplier<Object> {
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
}
