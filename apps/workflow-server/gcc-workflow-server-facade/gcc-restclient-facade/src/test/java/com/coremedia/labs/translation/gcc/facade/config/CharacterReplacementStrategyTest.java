package com.coremedia.labs.translation.gcc.facade.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterReplacementStrategyTest {
  @Nested
  @DisplayName("fromString(String):Optional<CharacterReplacementStrategy>")
  class MethodFromString {
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', nullValues = "null", textBlock = """
      input              | expected
      none               | NONE
      NoNe               | NONE
      EMPTY              | EMPTY
      underscore         | UNDERSCORE
      QUESTION_MARK      | QUESTION_MARK
      question-mark      | QUESTION_MARK
      questionmark       | QUESTION_MARK
      unicode-code-point | UNICODE_CODE_POINT
      unknown            | null
      """)
    void shouldParseStrategiesFormatTolerant(@NonNull String input, @Nullable CharacterReplacementStrategy expected) {
      Optional<CharacterReplacementStrategy> actual = CharacterReplacementStrategy.fromString(input);
      if (expected != null) {
        assertThat(actual).hasValue(expected);
      } else {
        assertThat(actual).isEmpty();
      }
    }
  }

  @Nested
  @DisplayName("fromConfig(Object):Optional<CharacterReplacementStrategy>")
  class MethodFromConfig {
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvSource(useHeadersInDisplayName = true, delimiter = '|', nullValues = "null", textBlock = """
      input      | expected
      none       | NONE
      NoNe       | NONE
      EMPTY      | EMPTY
      underscore | UNDERSCORE
      unknown    | null
      null       | null
      """)
    void shouldAcceptConfigStringValues(@Nullable String input, @Nullable CharacterReplacementStrategy expected) {
      Optional<CharacterReplacementStrategy> actual = CharacterReplacementStrategy.fromConfig(input);
      if (expected != null) {
        assertThat(actual).hasValue(expected);
      } else {
        assertThat(actual).isEmpty();
      }
    }

    @ParameterizedTest
    @EnumSource(CharacterReplacementStrategy.class)
    void shouldAcceptConfigEnumValues(@NonNull CharacterReplacementStrategy input) {
      Optional<CharacterReplacementStrategy> actual = CharacterReplacementStrategy.fromConfig(input);
      assertThat(actual).hasValue(input);
    }
  }

  @Nested
  @DisplayName("replacer():Function<MatchResult,String>")
  class MethodReplacer {
    @ParameterizedTest
    @EnumSource(CharacterReplacementStrategy.class)
    void shouldReturnReplacerFunction(@NonNull CharacterReplacementStrategy input) {
      assertThat(input.replacer()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(
      value = CharacterReplacementStrategy.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"NONE", "EMPTY"}
    )
    void shouldReplaceSomething(@NonNull CharacterReplacementStrategy input) {
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
    void shouldNotReplaceEmptyMatch(@NonNull CharacterReplacementStrategy input) {
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
    void shouldApplyReplacementsAsExpected(@NonNull CharacterReplacementStrategy strategy,
                                           @NonNull PatternFixture patternFixture,
                                           @NonNull String input,
                                           @NonNull String expected) {
      Function<MatchResult, String> replacer = strategy.replacer();
      Matcher matcher = patternFixture.matcher(input);
      String result = matcher.replaceAll(replacer);
      assertThat(result).isEqualTo(expected);
    }
  }

  enum PatternFixture {
    ANY(Pattern.compile(".*")),
    ANY_CHAR(Pattern.compile(".")),
    A_CHAR(Pattern.compile("a")),
    SMP(Pattern.compile("[^\\x00-\\uffff]"));

    private final Pattern pattern;

    PatternFixture(@NonNull Pattern pattern) {
      this.pattern = pattern;
    }

    @NonNull
    public Pattern pattern() {
      return pattern;
    }

    @NonNull
    public Matcher matcher(@NonNull CharSequence input) {
      return pattern.matcher(input);
    }
  }
}
