package com.coremedia.labs.translation.gcc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Map;

/**
 * Service Provider Interface for GCExchange facades.
 */
@DefaultAnnotation(NonNull.class)
public interface GCExchangeFacadeProvider {
  /**
   * Type token this SPI responds to.
   *
   * @return type token
   */
  String getTypeToken();

  /**
   * Signal if this SPI is responsible for the given type token.
   *
   * @param typeToken type token from settings
   * @return {@code true}, if {@link #getFacade(Map)} shall be called; {@code false} otherwise
   * @implNote By default compares to {@link #getTypeToken()} ignoring case.
   */
  default boolean isApplicable(String typeToken) {
    return getTypeToken().equalsIgnoreCase(typeToken);
  }

  /**
   * Get an instance of the facade.
   *
   * @param settings settings to use, contains for example credentials
   * @return GCExchange facade
   */
  GCExchangeFacade getFacade(Map<String, Object> settings);

  /**
   * Signal, if this is a default SPI, which should be used if no other SPI
   * returned {@code true} for {@link #isApplicable(String)}.
   *
   * @return {@code true} to make this SPI the default provider; {@code false} otherwise
   * @implNote Returns {@code false} by default
   * @implSpec Some SPI returning {@code true} will be considered as default as well as fallback for unknown type tokens.
   * If multiple SPI return {@code true} one of them will be taken without guarantee, which one.
   */
  default boolean isDefault() {
    return false;
  }
}
