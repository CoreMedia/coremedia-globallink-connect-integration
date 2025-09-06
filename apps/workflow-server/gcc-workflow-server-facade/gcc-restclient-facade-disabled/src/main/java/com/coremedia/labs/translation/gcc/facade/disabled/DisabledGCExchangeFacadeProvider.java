package com.coremedia.labs.translation.gcc.facade.disabled;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public class DisabledGCExchangeFacadeProvider implements GCExchangeFacadeProvider {
  private static final String TYPE_TOKEN = "disabled";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public GCExchangeFacade getFacade(Map<String, Object> settings) {
    return DisabledGCExchangeFacade.getInstance();
  }
}
