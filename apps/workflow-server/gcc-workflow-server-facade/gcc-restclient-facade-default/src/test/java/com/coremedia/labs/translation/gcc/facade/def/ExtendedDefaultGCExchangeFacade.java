package com.coremedia.labs.translation.gcc.facade.def;

import com.coremedia.labs.translation.gcc.util.Settings;
import org.gs4tr.gcc.restclient.model.GCFile;
import org.gs4tr.gcc.restclient.operation.Content;
import org.gs4tr.gcc.restclient.request.PageableRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An extended variant of the default facade, tailored for testing purpose.
 */
@NullMarked
public class ExtendedDefaultGCExchangeFacade extends DefaultGCExchangeFacade {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private volatile @Nullable ConnectorsConfig connectorsConfig;

  public ExtendedDefaultGCExchangeFacade(Map<String, Object> config) {
    super(new Settings(config));

    if (LOG.isDebugEnabled()) {
      LOG.debug("Connectors Configuration: {}", connectorsConfig());
    }
  }

  public static ExtendedDefaultGCExchangeFacade connect(Map<String, Object> config) {
    return new ExtendedDefaultGCExchangeFacade(config);
  }

  public ConnectorsConfig connectorsConfig() {
    ConnectorsConfig result = connectorsConfig;
    if (result == null) {
      synchronized (this) {
        result = connectorsConfig;
        if (result == null) {
          connectorsConfig = result = ConnectorsConfig.of(getDelegate().getConnectorsConfig());
        }
      }
    }
    return result;
  }

  public long totalRecordsCount() {
    Content.ContentResponseData contentList = getDelegate().getContentList();
    return contentList.getTotalRecordsCount();
  }

  public List<UploadedFile> getContentList() {
    List<GCFile> rawContentList = getDelegate().getContentList(new PageableRequest(1L, totalRecordsCount())).getResponseData();
    return rawContentList.stream()
      .map(UploadedFile::of)
      .toList();
  }
}
