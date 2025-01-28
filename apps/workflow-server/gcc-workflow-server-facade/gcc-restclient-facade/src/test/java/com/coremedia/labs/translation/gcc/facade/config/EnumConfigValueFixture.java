package com.coremedia.labs.translation.gcc.facade.config;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

  @NonNull
  private final Function<Enum<?>, String> toConfigValue;

  EnumConfigValueFixture(@NonNull Function<Enum<?>, String> toConfigValue) {
    this.toConfigValue = toConfigValue;
  }

  @NonNull
  public String toConfigValue(@NonNull Enum<?> transform) {
    return toConfigValue.apply(transform);
  }

  @NonNull
  public static Stream<? extends Arguments> provideArguments(@NonNull Enum<?>[] enumType) {
    return java.util.stream.Stream.of(enumType)
      .flatMap(en -> java.util.stream.Stream.of(values())
        .map(testCaseFixture -> Arguments.of(en, testCaseFixture.toConfigValue(en))));
  }
}
