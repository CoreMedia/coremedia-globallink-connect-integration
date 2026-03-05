package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider;
import com.coremedia.labs.translation.gcc.util.Settings;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultGCExchangeFacadeProvider implements GCExchangeFacadeProvider {
  private static final String TYPE_TOKEN = "default";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public boolean isDefault() {
    return true;
  }

  @Override
  public GCExchangeFacade getFacade(Settings settings) {
    return new DefaultGCExchangeFacade(settings);
  }

}
