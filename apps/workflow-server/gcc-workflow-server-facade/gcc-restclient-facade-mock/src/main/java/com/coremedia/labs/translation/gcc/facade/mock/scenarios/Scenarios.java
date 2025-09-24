package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Utility for discovering available {@link Scenario}s.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public enum Scenarios {
  ;

  /**
   * Returns the {@code Scenario} matching the given identifier.
   * <p>
   * Discovery of providers is lazy; only providers up to and including the
   * match are instantiated.
   *
   * @param scenarioId the non-empty scenario identifier
   * @return an {@code Optional} containing the matching scenario, otherwise
   * empty
   */
  public static Optional<Scenario> fromString(String scenarioId) {
    if (scenarioId.isBlank()) {
      return Optional.empty();
    }
    return Holder.LOADER.stream()
      .map(ServiceLoader.Provider::get)
      .filter(s -> scenarioId.equalsIgnoreCase(s.id()))
      .findFirst();
  }

  /**
   * Lazy holder for the shared {@link ServiceLoader} instance.
   */
  private static final class Holder {
    private static final ServiceLoader<Scenario> LOADER = ServiceLoader.load(Scenario.class);
  }
}
