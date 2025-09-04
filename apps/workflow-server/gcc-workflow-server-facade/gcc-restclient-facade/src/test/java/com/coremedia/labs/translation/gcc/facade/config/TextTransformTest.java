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
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.util.stream.Stream;

import static com.coremedia.labs.translation.gcc.facade.config.TextTransform.TEXT_TO_HTML;
import static org.assertj.core.api.Assertions.assertThat;

class TextTransformTest {
  @Nested
  class MethodTransform {
    @Nested
    class TransformTextToHtml {
      @ParameterizedTest
      @DisplayName("textToHtml: Text should be transformed to HTML")
      @EnumSource(TextToHtmlFixture.class)
      void shouldTransformTextToHtml(@NonNull TextToHtmlFixture fixture) {
        String actual = TEXT_TO_HTML.transform(fixture.input());
        assertThat(actual).isEqualTo(fixture.expectedOutput());
      }
    }

    @Nested
    class TransformNone {
      @ParameterizedTest
      @DisplayName("none: Text should not be transformed")
      @EnumSource(TextToHtmlFixture.class)
      void shouldNotTransformText(@NonNull TextToHtmlFixture fixture) {
        String actual = TextTransform.NONE.transform(fixture.input());
        assertThat(actual).isEqualTo(fixture.input());
      }
    }
  }

  @Nested
  class FactoryMethods {
    @Nested
    @DisplayName("fromString(String):Optional<TextTransform>")
    class FromString {
      @ParameterizedTest
      @ArgumentsSource(ConfigTestCaseFixtureProvider.class)
      void shouldParseValues(@NonNull TextTransform expected, @NonNull String configValue) {
        assertThat(TextTransform.fromString(configValue)).hasValue(expected);
      }

      @Test
      void shouldReturnEmptyOnNull() {
        assertThat(TextTransform.fromString(null)).isEmpty();
      }

      @Test
      void shouldReturnEmptyOnUnknownType() {
        assertThat(TextTransform.fromConfig("unknown")).isEmpty();
      }
    }

    @Nested
    class FromConfig {
      @ParameterizedTest
      @ArgumentsSource(ConfigTestCaseFixtureProvider.class)
      void shouldParseValues(@NonNull TextTransform expected, @NonNull String configValue) {
        assertThat(TextTransform.fromConfig(configValue)).hasValue(expected);
      }

      @Test
      void shouldReturnEmptyOnNull() {
        assertThat(TextTransform.fromConfig(null)).isEmpty();
      }

      @Test
      void shouldReturnEmptyOnUnknownType() {
        assertThat(TextTransform.fromConfig(42)).isEmpty();
      }

      @ParameterizedTest
      @EnumSource(TextTransform.class)
      void shouldReturnEnumAsIs(@NonNull TextTransform input) {
        TextTransform actual = TextTransform.fromConfig(input).orElse(null);
        assertThat(actual).isEqualTo(input);
      }
    }
  }

  static class ConfigTestCaseFixtureProvider implements ArgumentsProvider {
    @Override
    public @NonNull Stream<? extends Arguments> provideArguments(@NonNull ParameterDeclarations parameters,
                                                                 @NonNull ExtensionContext context) {
      return EnumConfigValueFixture.provideArguments(TextTransform.values());
    }
  }

  enum TextToHtmlFixture {
    AMPERSAND("&", "&amp;"),
    LESS_THAN("<", "&lt;"),
    GREATER_THAN(">", "&gt;"),
    QUOTATION_MARK("\"", "&quot;"),
    NEWLINE("\n", "<br>"),
    CARRIAGE_RETURN("\r", "<br>"),
    CARRIAGE_RETURN_NEWLINE("\r\n", "<br>"),
    LEADING_SPACES("  text", "&nbsp;&nbsp;text"),
    LEADING_SPACES_MULTILINE("  text\n  text", "&nbsp;&nbsp;text<br>&nbsp;&nbsp;text"),
    TABS("\ttext", "&nbsp;&nbsp;&nbsp;&nbsp;text"),
    TABS_MULTILINE("\ttext\n\ttext", "&nbsp;&nbsp;&nbsp;&nbsp;text<br>&nbsp;&nbsp;&nbsp;&nbsp;text"),
    ;

    private final String text;
    private final String html;

    TextToHtmlFixture(String text, String html) {
      this.text = text;
      this.html = html;
    }

    public String input() {
      return text;
    }

    public String expectedOutput() {
      return html;
    }
  }
}
