package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import com.coremedia.cap.struct.StructService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.CT_SITE_CONTENT;
import static java.util.Objects.requireNonNull;

@NullMarked
public class GlobalLinkConfigBuilder {
  public static final String CT_GLOBAL_CONFIG = "SimpleStruct";
  public static final String P_GLOBAL_CONFIG = "value";
  public static final String CT_SITE_CONFIG = CT_SITE_CONTENT;
  public static final String P_SITE_CONFIG = "struct";

  private final StructService structService;
  private final ContentRepository repository;
  private RetryDelayMode retryDelayMode = RetryDelayMode.INTEGER;
  private @Nullable Site site;
  private final Map<String, Duration> retryDelays = new HashMap<>();

  public GlobalLinkConfigBuilder(ContentRepository repository,
                                 StructService structService) {
    this.structService = requireNonNull(structService);
    this.repository = requireNonNull(repository);
  }

  public GlobalLinkConfigBuilder atGlobal() {
    site = null;
    return this;
  }

  public GlobalLinkConfigBuilder atSite(Site site) {
    this.site = requireNonNull(site);
    return this;
  }

  public GlobalLinkConfigBuilder withRetryDelayMode(RetryDelayMode retryDelayMode) {
    this.retryDelayMode = requireNonNull(retryDelayMode);
    return this;
  }

  public GlobalLinkConfigBuilder withRetryDelay(String key, Duration retryDelay) {
    retryDelays.put(requireNonNull(key), requireNonNull(retryDelay));
    return this;
  }

  public void build() {
    StructBuilder configBuilder = structService.createStructBuilder()
      .enter("globalLink");
    retryDelays.forEach((key, retryDelay) -> retryDelayMode.apply(configBuilder, key, retryDelay));
    Struct config = configBuilder.build();
    if (site == null) {
      repository.createContentBuilder()
        .type(CT_GLOBAL_CONFIG)
        .name(GlobalLinkAction.GLOBAL_CONFIGURATION_PATH)
        .property(P_GLOBAL_CONFIG, config)
        .checkedIn()
        .create();
    } else {
      repository.createContentBuilder()
        .type(CT_SITE_CONFIG)
        .parent(site.getSiteRootFolder())
        .name(GlobalLinkAction.SITE_CONFIGURATION_PATH)
        .property(P_SITE_CONFIG, config)
        .checkedIn()
        .create();
    }
  }

  public enum RetryDelayMode {
    INTEGER {
      @Override
      void apply(StructBuilder structBuilder, String key, Duration duration) {
        structBuilder
          .declareInteger(key, Math.toIntExact(duration.toSeconds()));
      }
    },
    INTEGER_AS_STRING {
      @Override
      void apply(StructBuilder structBuilder, String key, Duration duration) {
        structBuilder
          .declareString(key, Integer.MAX_VALUE, Integer.toString(Math.toIntExact(duration.toSeconds())));
      }
    },
    DURATION_AS_STRING {
      @Override
      void apply(StructBuilder structBuilder, String key, Duration duration) {
        structBuilder
          .declareString(key, Integer.MAX_VALUE, DurationFormatterUtils.print(duration, DurationFormat.Style.COMPOSITE));
      }
    };

    abstract void apply(StructBuilder structBuilder, String key, Duration duration);
  }
}
