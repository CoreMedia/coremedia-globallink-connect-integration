/**
 * This package contains scenarios that can be triggered via setting
 * {@code mock.scenario} to customize the behavior of the GCC mock client.
 * <p>
 * All scenarios must implement the
 * {@link com.coremedia.labs.translation.gcc.facade.mock.scenarios.Scenario}
 * interface. Find more details for implementing scenarios there.
 * <p>
 * Scenarios are typically used during demonstration, for testing purposes,
 * or to update screenshots of the UI.
 * <p>
 * These main architectural elements are involved:
 * <p>
 * <strong>Interceptors:</strong>
 * These represent hooks into the main workflow of the mock GCC client.
 * Scenarios typically implement one or more of these interfaces.
 * <p>
 * <strong>Scenarios:</strong>
 * These represent behaviors suitable for development, manual testing (as
 * part of the approval process), demonstration, or screenshots.
 * <p>
 * <strong>Service Loader:</strong>
 * Scenarios are registered using Java's Service Loader mechanism.
 * This again is managed by
 * {@link com.coremedia.labs.translation.gcc.facade.mock.scenarios.Scenarios}.
 *
 * @see com.coremedia.labs.translation.gcc.facade.mock.scenarios.Scenario
 * @since 2506.0.1-1
 */
package com.coremedia.labs.translation.gcc.facade.mock.scenarios;
