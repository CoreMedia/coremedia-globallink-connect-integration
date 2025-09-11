package com.coremedia.labs.translation.gcc.facade.config;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@NullMarked
class CharacterReplacementStrategyTest {
  @Nested
  @DisplayName("replacer():Function<MatchResult,String>")
  class MethodReplacer {
    @ParameterizedTest
    @EnumSource(CharacterReplacementStrategy.class)
    void shouldReturnReplacerFunction(CharacterReplacementStrategy input) {
      assertThat(input.replacer()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(
      value = CharacterReplacementStrategy.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"NONE", "EMPTY"}
    )
    void shouldReplaceSomething(CharacterReplacementStrategy input) {
      Function<MatchResult, String> replacer = input.replacer();
      String originalString = "a";
      Matcher matcher = PatternFixture.ANY_CHAR.matcher(originalString);
      String result = matcher.replaceAll(replacer);
      assertThat(result)
        .isNotEmpty()
        .isNotEqualTo(originalString);
    }

    @ParameterizedTest
    @EnumSource(
      value = CharacterReplacementStrategy.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"NONE", "EMPTY"}
    )
    void shouldNotReplaceEmptyMatch(CharacterReplacementStrategy input) {
      Function<MatchResult, String> replacer = input.replacer();
      String originalString = "";
      Matcher matcher = PatternFixture.ANY.matcher(originalString);
      String result = matcher.replaceAll(replacer);
      assertThat(result)
        .isEmpty();
    }

    @Test
    void noneShouldReplaceNothing() {
      Function<MatchResult, String> replacer = CharacterReplacementStrategy.NONE.replacer();
      String originalString = "a";
      Matcher matcher = PatternFixture.ANY_CHAR.matcher(originalString);
      String result = matcher.replaceAll(replacer);
      assertThat(result)
        .isEqualTo(originalString);
    }

    @Test
    void emptyShouldRemoveMatches() {
      Function<MatchResult, String> replacer = CharacterReplacementStrategy.EMPTY.replacer();
      String originalString = "a";
      Matcher matcher = PatternFixture.ANY_CHAR.matcher(originalString);
      String result = matcher.replaceAll(replacer);
      assertThat(result)
        .isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', textBlock = """
      strategy           | pattern  | input              | expected
      UNDERSCORE         | ANY_CHAR | 'ab'               | '__'
      UNDERSCORE         | A_CHAR   | 'ab'               | '_b'
      UNDERSCORE         | SMP      | 'a\uD83D\uDD4Ab'   | 'a_b'
      QUESTION_MARK      | ANY_CHAR | 'a'                | '?'
      UNICODE_CODE_POINT | ANY_CHAR | 'a'                | 'U+0061'
      UNICODE_CODE_POINT | SMP      | 'a\uD83D\uDD4A'    | 'aU+1F54A'
      """)
    void shouldApplyReplacementsAsExpected(CharacterReplacementStrategy strategy,
                                           PatternFixture patternFixture,
                                           String input,
                                           String expected) {
      Function<MatchResult, String> replacer = strategy.replacer();
      Matcher matcher = patternFixture.matcher(input);
      String result = matcher.replaceAll(replacer);
      assertThat(result).isEqualTo(expected);
    }
  }

  @Nested
  class FactoryMethods {
    @Nested
    @DisplayName("fromString(String):Optional<CharacterReplacementStrategy>")
    class FromString {
      @ParameterizedTest
      @ArgumentsSource(ConfigTestCaseFixtureProvider.class)
      void shouldParseValues(CharacterReplacementStrategy expected, String configValue) {
        assertThat(CharacterReplacementStrategy.fromString(configValue)).hasValue(expected);
      }

      @Test
      void shouldReturnEmptyOnNull() {
        assertThat(CharacterReplacementStrategy.fromString(null)).isEmpty();
      }

      @Test
      void shouldReturnEmptyOnUnknownType() {
        assertThat(CharacterReplacementStrategy.fromConfig("unknown")).isEmpty();
      }
    }

    @Nested
    class FromConfig {
      @ParameterizedTest
      @ArgumentsSource(ConfigTestCaseFixtureProvider.class)
      void shouldParseValues(CharacterReplacementStrategy expected, String configValue) {
        assertThat(CharacterReplacementStrategy.fromConfig(configValue)).hasValue(expected);
      }

      @Test
      void shouldReturnEmptyOnNull() {
        assertThat(CharacterReplacementStrategy.fromConfig(null)).isEmpty();
      }

      @Test
      void shouldReturnEmptyOnUnknownType() {
        assertThat(CharacterReplacementStrategy.fromConfig(42)).isEmpty();
      }

      @ParameterizedTest
      @EnumSource(CharacterReplacementStrategy.class)
      void shouldReturnEnumAsIs(CharacterReplacementStrategy input) {
        CharacterReplacementStrategy actual = CharacterReplacementStrategy.fromConfig(input).orElse(null);
        assertThat(actual).isEqualTo(input);
      }
    }
  }

  enum PatternFixture {
    ANY(Pattern.compile(".*")),
    ANY_CHAR(Pattern.compile(".")),
    A_CHAR(Pattern.compile("a")),
    SMP(Pattern.compile("[^\\x00-\\uffff]"));

    private final Pattern pattern;

    PatternFixture(Pattern pattern) {
      this.pattern = pattern;
    }

    public Matcher matcher(CharSequence input) {
      return pattern.matcher(input);
    }
  }

  static class ConfigTestCaseFixtureProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters,
                                                        ExtensionContext context) {
      return EnumConfigValueFixture.provideArguments(CharacterReplacementStrategy.values());
    }
  }
}
