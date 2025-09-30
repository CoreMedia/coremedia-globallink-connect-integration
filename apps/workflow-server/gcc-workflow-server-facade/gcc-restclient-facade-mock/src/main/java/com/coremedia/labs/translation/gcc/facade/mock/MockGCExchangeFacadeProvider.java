package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider;
import com.coremedia.labs.translation.gcc.util.Settings;
import com.google.common.annotations.VisibleForTesting;
import org.jspecify.annotations.NullMarked;

import static com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings.fromGlobalLinkConfig;

@NullMarked
public class MockGCExchangeFacadeProvider implements GCExchangeFacadeProvider {
  @VisibleForTesting
  static final String TYPE_TOKEN = "mock";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public GCExchangeFacade getFacade(Settings settings) {
    return new MockedGCExchangeFacade(fromGlobalLinkConfig(settings));
  }
}
