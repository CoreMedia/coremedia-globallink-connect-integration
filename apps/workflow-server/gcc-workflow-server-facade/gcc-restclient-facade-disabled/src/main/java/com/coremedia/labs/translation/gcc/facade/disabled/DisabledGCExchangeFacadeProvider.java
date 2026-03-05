package com.coremedia.labs.translation.gcc.facade.disabled;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider;
import com.coremedia.labs.translation.gcc.util.Settings;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DisabledGCExchangeFacadeProvider implements GCExchangeFacadeProvider {
  private static final String TYPE_TOKEN = "disabled";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public GCExchangeFacade getFacade(Settings settings) {
    return DisabledGCExchangeFacade.getInstance();
  }
}
