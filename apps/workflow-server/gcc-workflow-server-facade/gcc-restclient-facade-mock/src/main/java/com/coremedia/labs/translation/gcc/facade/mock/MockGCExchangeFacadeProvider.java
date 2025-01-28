package com.coremedia.labs.translation.gcc.facade.mock;

import com.coremedia.labs.translation.gcc.facade.GCExchangeFacade;
import com.coremedia.labs.translation.gcc.facade.GCExchangeFacadeProvider;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Map;

import static com.coremedia.labs.translation.gcc.facade.mock.settings.MockSettings.fromGlobalLinkConfig;

@DefaultAnnotation(NonNull.class)
public class MockGCExchangeFacadeProvider implements GCExchangeFacadeProvider {
  @VisibleForTesting
  static final String TYPE_TOKEN = "mock";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public GCExchangeFacade getFacade(Map<String, Object> settings) {
    return new MockedGCExchangeFacade(fromGlobalLinkConfig(settings));
  }
}
