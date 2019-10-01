package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Map;

@DefaultAnnotation(NonNull.class)
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
  public GCExchangeFacade getFacade(Map<String, String> settings) {
    return new DefaultGCExchangeFacade(settings);
  }

}
