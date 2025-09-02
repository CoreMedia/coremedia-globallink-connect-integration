package com.coremedia.labs.translation.gcc.workflow;

import com.coremedia.cap.content.ContentRepository;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.struct.StructBuilder;
import com.coremedia.cap.struct.StructService;
import com.coremedia.labs.translation.gcc.util.Settings;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.coremedia.labs.translation.gcc.workflow.SimpleMultiSiteConfiguration.CT_SITE_CONTENT;
import static java.util.Objects.requireNonNull;

public class GlobalLinkConfigBuilder {
  public static final String CT_GLOBAL_CONFIG = "SimpleStruct";
  public static final String P_GLOBAL_CONFIG = "value";
  public static final String CT_SITE_CONFIG = CT_SITE_CONTENT;
  public static final String P_SITE_CONFIG = "struct";

  @NonNull
  private final StructService structService;
  @NonNull
  private final ContentRepository repository;
  @NonNull
  private RetryDelayMode retryDelayMode = RetryDelayMode.INTEGER;
  @Nullable
  private Site site;
  @NonNull
  private final Map<String, Duration> retryDelays = new HashMap<>();

  public GlobalLinkConfigBuilder(@NonNull ContentRepository repository,
                                 @NonNull StructService structService) {
    this.structService = requireNonNull(structService);
    this.repository = requireNonNull(repository);
  }

  @NonNull
  public GlobalLinkConfigBuilder atGlobal() {
    site = null;
    return this;
  }

  @NonNull
  public GlobalLinkConfigBuilder atSite(@NonNull Site site) {
    this.site = requireNonNull(site);
    return this;
  }

  @NonNull
  public GlobalLinkConfigBuilder withRetryDelayMode(@NonNull RetryDelayMode retryDelayMode) {
    this.retryDelayMode = requireNonNull(retryDelayMode);
    return this;
  }

  @NonNull
  public GlobalLinkConfigBuilder withRetryDelay(@NonNull String key, @NonNull Duration retryDelay) {
    retryDelays.put(requireNonNull(key), requireNonNull(retryDelay));
    return this;
  }

  public void build() {
    StructBuilder configBuilder = structService.createStructBuilder()
      .enter("globalLink");
    retryDelays.forEach((key, retryDelay) -> {
      retryDelayMode.apply(configBuilder, key, retryDelay);
    });
    Struct config = configBuilder.build();
    if (site == null) {
      repository.createContentBuilder()
        .type(CT_GLOBAL_CONFIG)
        .name(Settings.GLOBAL_CONFIGURATION_PATH)
        .property(P_GLOBAL_CONFIG, config)
        .checkedIn()
        .create();
    } else {
      repository.createContentBuilder()
        .type(CT_SITE_CONFIG)
        .parent(site.getSiteRootFolder())
        .name(Settings.SITE_CONFIGURATION_PATH)
        .property(P_SITE_CONFIG, config)
        .checkedIn()
        .create();
    }
  }

  public enum RetryDelayMode {
    INTEGER {
      @Override
      void apply(@NonNull StructBuilder structBuilder, @NonNull String key, @NonNull Duration duration) {
        structBuilder
          .declareInteger(key, Math.toIntExact(duration.toSeconds()));
      }
    },
    INTEGER_AS_STRING {
      @Override
      void apply(@NonNull StructBuilder structBuilder, @NonNull String key, @NonNull Duration duration) {
        structBuilder
          .declareString(key, Integer.MAX_VALUE, Integer.toString(Math.toIntExact(duration.toSeconds())));
      }
    },
    DURATION_AS_STRING {
      @Override
      void apply(@NonNull StructBuilder structBuilder, @NonNull String key, @NonNull Duration duration) {
        structBuilder
          .declareString(key, Integer.MAX_VALUE, DurationFormatterUtils.print(duration, DurationFormat.Style.COMPOSITE));
      }
    };

    abstract void apply(@NonNull StructBuilder structBuilder, @NonNull String key, @NonNull Duration duration);
  }
}
