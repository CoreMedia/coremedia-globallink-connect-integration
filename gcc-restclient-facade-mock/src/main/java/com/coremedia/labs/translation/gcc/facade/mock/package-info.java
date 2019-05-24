/**
 * <p>
 * Contains classes for mock translation. Main entry point is the mock facade:
 * {@link com.coremedia.labs.translation.gcc.facade.mock.MockedGCExchangeFacade}. All
 * other classes are meant to support mocking and to keep state in between
 * the different calls to the facade.
 * </p>
 * <p>
 * The class {@link com.coremedia.labs.translation.gcc.facade.mock.Task} contains
 * some intended latency to simulate that translation of contents takes some
 * time.
 * </p>
 */
package com.coremedia.labs.translation.gcc.facade.mock;
