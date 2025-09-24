package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import org.jspecify.annotations.NullMarked;

/**
 * A scenario that does nothing.
 * Used as a default scenario.
 * <p>
 * Typically not referenced directly as a scenario.
 *
 * @since 2506.0.1-1
 */
@NullMarked
public class NoOperationScenario implements Scenario {
  /**
   * The identifier of this scenario.
   */
  static final String ID = "no-operation";
  public static final NoOperationScenario INSTANCE = new NoOperationScenario();

  @Override
  public String id() {
    return ID;
  }
}
