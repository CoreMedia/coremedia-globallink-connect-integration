// New file: `SettingsCollectors.java`
package com.coremedia.labs.translation.gcc.util;

import edu.umd.cs.findbugs.annotations.Nullable;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Collectors for {@link Settings}.
 *
 * @since 2506.0.0-1
 */
public enum SettingsCollectors {
  ;

  /**
   * Merge all streamed {@code Settings}.
   */
  public static Collector<Settings, ?, Settings> merging() {
    return merging(Settings.EMPTY);
  }

  /**
   * Merge all streamed {@code Settings} into an optional initial base.
   *
   * @param base initial base; if {@link Settings#EMPTY} acts as "no base"
   */
  public static Collector<Settings, ?, Settings> merging(Settings base) {
    return new MergingSettingsCollector(base);
  }

  /**
   * A mutable holder of the current value.
   */
  private static final class Holder {
    @Nullable
    private Settings value;

    Holder(@Nullable Settings initial) {
      value = initial;
    }
  }

  private record MergingSettingsCollector(Settings base) implements Collector<Settings, Holder, Settings> {
    @Override
    public Supplier<Holder> supplier() {
      return () -> new Holder(base.isEmpty() ? null : base);
    }

    @Override
    public BiConsumer<Holder, Settings> accumulator() {
      return (h, s) -> {
        if (h.value == null) {
          h.value = s;
        } else {
          h.value = h.value.putAll(s);
        }
      };
    }

    @Override
    public BinaryOperator<Holder> combiner() {
      return (h1, h2) -> {
        if (h1.value == null) {
          return h2;
        }
        if (h2.value == null) {
          return h1;
        }
        h1.value = h1.value.putAll(h2.value);
        return h1;
      };
    }

    @Override
    public Function<Holder, Settings> finisher() {
      return h -> h.value == null ? Settings.EMPTY : h.value;
    }

    @Override
    public Set<Characteristics> characteristics() {
      // Order matters; no UNORDERED / IDENTITY_FINISH.
      return Set.of();
    }
  }
}
