package com.coremedia.labs.translation.gcc.facade.def;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Requires a test to have GCC credentials at hand. If they are not available,
 * the extended tests are ignored. If they are available, a test may get the
 * read credentials injected into a {@code Map<String, String>} parameter.
 */
@NullMarked
public class GccCredentialsExtension implements ExecutionCondition, ParameterResolver {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final String USER_HOME = System.getProperty("user.home");
  private static final String GCC_PROPERTIES_DEFAULT = ".gcc.properties";
  @SuppressWarnings("InlineFormatString")
  private static final String GCC_PROPERTIES_PROFILE_PATTERN = ".gcc.%s.properties";
  private static final Path GCC_CREDENTIALS_PATH = Paths.get(USER_HOME, GCC_PROPERTIES_DEFAULT);
  private @Nullable Map<String, String> gccProperties;

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext extensionContext) {
    synchronized (this) {
      if (gccProperties != null) {
        return ConditionEvaluationResult.enabled("GCC Properties already read.");
      }
    }
    Path configPath = GCC_CREDENTIALS_PATH;
    if (!Files.exists(configPath)) {
      return ConditionEvaluationResult.disabled("Missing properties file: " + configPath + ". Test disabled.");
    }

    try {
      Map<String, String> propertiesMap = readConfiguration(configPath);
      synchronized (this) {
        gccProperties = propertiesMap;
      }
      return ConditionEvaluationResult.enabled("GCC Properties read from " + configPath);
    } catch (IOException e) {
      LOG.error("Failed to read properties file: {}", configPath, e);
      return ConditionEvaluationResult.disabled("Failed to read properties file: " + configPath + ". Test disabled. Cause: " + e.getMessage());
    }
  }

  private static Map<String, String> readConfiguration(Path configPath) throws IOException {
    Properties properties = new Properties();
    try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties.entrySet().stream()
      .collect(toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    return Map.class.isAssignableFrom(parameter.getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    Map<String, String> baseProperties;

    synchronized (this) {
      baseProperties = gccProperties;
    }

    if (baseProperties == null) {
      baseProperties = new HashMap<>();
    } else {
      baseProperties = new HashMap<>(baseProperties);
    }

    Map<String, String> result = baseProperties;

    // Prefer annotation at the parameter, then use that of the enclosing
    // method or class.
    parameterContext.findAnnotation(GccCredentials.class)
      .map(GccCredentials::value)
      .filter(v -> !v.isEmpty())
      .or(() -> extensionContext.getElement()
        .flatMap(e -> AnnotationSupport.findAnnotation(e, GccCredentials.class))
        .map(GccCredentials::value)
      )
      .ifPresent(profile -> {
        Map<String, String> profileConfiguration = getProfileConfiguration(profile);
        if (!profileConfiguration.isEmpty()) {
          result.putAll(profileConfiguration);
          LOG.info("Overriding configuration with data from profile {}", profile);
        }
      });

    return result;
  }

  private static Map<String, String> getProfileConfiguration(String profile) {
    Path configPath = Paths.get(USER_HOME, GCC_PROPERTIES_PROFILE_PATTERN.formatted(profile));
    if (Files.isRegularFile(configPath) && Files.isReadable(configPath)) {
      try {
        return readConfiguration(configPath);
      } catch (IOException e) {
        LOG.warn("Failed to read profile's properties file: {}", configPath, e);
      }
    }
    return Map.of();
  }
}
