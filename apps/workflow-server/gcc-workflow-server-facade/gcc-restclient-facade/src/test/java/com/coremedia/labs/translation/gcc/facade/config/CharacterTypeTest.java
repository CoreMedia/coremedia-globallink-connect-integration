package com.coremedia.labs.translation.gcc.facade.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterTypeTest {
  @Nested
  class MethodReplaceAllInvalid {
    @Test
    void shouldReturnAsIsForCharacterTypeUnicode() {
      String input = "Unicode Dove: \uD83D\uDC99!";
      String actual = CharacterType.UNICODE.replaceAllInvalid(input, m -> "");
      assertThat(actual).isEqualTo(input);
    }

    @Test
    void shouldReplaceInvalidCharactersForCharacterTypeBmp() {
      String input = "Max Basic Plane: \uFFFF, Supplementary Plane Dove: \uD83E\uDD8A!";
      String actual = CharacterType.BMP.replaceAllInvalid(input, m -> "");
      assertThat(actual).isEqualTo("Max Basic Plane: \uFFFF, Supplementary Plane Dove: !");
    }
  }

  @Nested
  class FactoryMethods {
    @Nested
    @DisplayName("fromString(String):Optional<CharacterType>")
    class FromString {
      @ParameterizedTest
      @ArgumentsSource(ConfigTestCaseFixtureProvider.class)
      void shouldParseValues(@NonNull CharacterType expected, @NonNull String configValue) {
        assertThat(CharacterType.fromString(configValue)).hasValue(expected);
      }

      @Test
      void shouldReturnEmptyOnNull() {
        assertThat(CharacterType.fromString(null)).isEmpty();
      }

      @Test
      void shouldReturnEmptyOnUnknownType() {
        assertThat(CharacterType.fromConfig("unknown")).isEmpty();
      }
    }

    @Nested
    class FromConfig {
      @ParameterizedTest
      @ArgumentsSource(ConfigTestCaseFixtureProvider.class)
      void shouldParseValues(@NonNull CharacterType expected, @NonNull String configValue) {
        assertThat(CharacterType.fromConfig(configValue)).hasValue(expected);
      }

      @Test
      void shouldReturnEmptyOnNull() {
        assertThat(CharacterType.fromConfig(null)).isEmpty();
      }

      @Test
      void shouldReturnEmptyOnUnknownType() {
        assertThat(CharacterType.fromConfig(42)).isEmpty();
      }

      @ParameterizedTest
      @EnumSource(CharacterType.class)
      void shouldReturnEnumAsIs(@NonNull CharacterType input) {
        CharacterType actual = CharacterType.fromConfig(input).orElse(null);
        assertThat(actual).isEqualTo(input);
      }
    }
  }

  static class ConfigTestCaseFixtureProvider implements ArgumentsProvider {
    @Override
    public @NonNull Stream<? extends Arguments> provideArguments(@NonNull ExtensionContext context) {
      return EnumConfigValueFixture.provideArguments(CharacterType.values());
    }
  }

}
