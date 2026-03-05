package com.coremedia.labs.translation.gcc.facade.config;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NullMarked
enum EnumConfigValueFixture {
  NAME() {
    @Override
    public String toConfigValue(Enum<?> transform) {
      return transform.name();
    }
  },
  LOWER_CASE() {
    @Override
    public String toConfigValue(Enum<?> transform) {
      return transform.name().toLowerCase(Locale.ROOT);
    }
  },
  UPPER_CASE() {
    @Override
    public String toConfigValue(Enum<?> transform) {
      return transform.name().toLowerCase(Locale.ROOT);
    }
  },
  CAMEL_CASE() {
    @Override
    public String toConfigValue(Enum<?> transform) {
      Pattern pattern = Pattern.compile("_(.)");
      Matcher matcher = pattern.matcher(transform.name().toLowerCase(Locale.ROOT));
      return matcher.replaceAll(m -> m.group(1).toUpperCase(Locale.ROOT));
    }
  },
  KEBAP_CASE() {
    @Override
    public String toConfigValue(Enum<?> transform) {
      return transform.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
  },
  ;

  public abstract String toConfigValue(Enum<?> transform);

  public static Stream<? extends Arguments> provideArguments(Enum<?>[] enumType) {
    return java.util.stream.Stream.of(enumType)
      .flatMap(en -> java.util.stream.Stream.of(values())
        .map(testCaseFixture -> Arguments.of(en, testCaseFixture.toConfigValue(en))));
  }
}
