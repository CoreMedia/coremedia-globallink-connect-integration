/**
 * <p>
 * This package contains a facade for the GCC Rest Client. While it is especially
 * used for mocking, it also serves as overview which parts of the GCC Rest Client
 * API are actually used.
 * </p>
 * <p>
 * The main entry-point to this package is
 * {@link com.coremedia.labs.translation.gcc.facade.DefaultGCExchangeFacadeSessionProvider} which
 * will open a session to GCC (or a mocked one) for you. The next important class
 * you should be aware of is the provided
 * {@link com.coremedia.labs.translation.gcc.facade.GCExchangeFacade}
 * which is the facade to do all subsequent communication once you have
 * established a connection.
 * </p>
 */
package com.coremedia.labs.translation.gcc.facade;
