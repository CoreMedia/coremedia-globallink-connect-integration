package com.coremedia.labs.translation.gcc.facade.internal;

import com.coremedia.blueprint.translation.TranslationService;
import com.coremedia.labs.translation.gcc.facade.GCConfigProperty;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider;
import com.coremedia.labs.translation.gcc.util.Settings;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Map;

@DefaultAnnotation(NonNull.class)
public class InternalGCExchangeFacadeProvider implements GCExchangeFacadeProvider {
  @VisibleForTesting
  static final String TYPE_TOKEN = "internal";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public GCExchangeFacade getFacade(Settings settings) {
    InternalGCExchangeFacade facade = new InternalGCExchangeFacade();
    settings.at("translationService")
      .ifPresent(translationService -> facade.setTranslationService((TranslationService) translationService));
    return facade;
  }
}
