package com.coremedia.labs.translation.gcc.facade.config;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@NullMarked
enum EnumConfigValueFixture {
  NAME(Enum::name),
  LOWER_CASE(en -> en.name().toLowerCase(Locale.ROOT)),
  UPPER_CASE(en -> en.name().toLowerCase(Locale.ROOT)),
  CAMEL_CASE(en -> {
    Pattern pattern = Pattern.compile("_(.)");
    Matcher matcher = pattern.matcher(en.name().toLowerCase(Locale.ROOT));
    return matcher.replaceAll(m -> m.group(1).toUpperCase(Locale.ROOT));
  }),
  KEBAP_CASE(en -> en.name().toLowerCase(Locale.ROOT).replace('_', '-')),
  ;

  private final Function<Enum<?>, String> toConfigValue;

  EnumConfigValueFixture(Function<Enum<?>, String> toConfigValue) {
    this.toConfigValue = toConfigValue;
  }

  public String toConfigValue(Enum<?> transform) {
    return toConfigValue.apply(transform);
  }

  public static Stream<? extends Arguments> provideArguments(Enum<?>[] enumType) {
    return java.util.stream.Stream.of(enumType)
      .flatMap(en -> java.util.stream.Stream.of(values())
        .map(testCaseFixture -> Arguments.of(en, testCaseFixture.toConfigValue(en))));
  }
}
