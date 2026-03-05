package com.coremedia.labs.translation.gcc.facade.mock.scenarios;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.lang.reflect.Modifier;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@NullMarked
class ScenariosTest {

  private static final String SCENARIOS_PACKAGE = "com.coremedia.labs.translation.gcc.facade.mock.scenarios";

  @ParameterizedTest
  @ArgumentsSource(ScenariosArgumentsProvider.class)
  void shouldProvideAllSupportedScenariosById(Scenario expectedScenario) {
    assertThat(Scenarios.fromString(expectedScenario.id()))
      .hasValueSatisfying(sc -> {
        Class<? extends Scenario> expectedClass = expectedScenario.getClass();
        Class<? extends Scenario> actualClass = sc.getClass();
        assertThat(actualClass).isEqualTo(expectedClass);
      });
  }

  @Test
  void allScenarioImplementationsAreRegisteredWithServiceLoader() {
    Set<Class<? extends Scenario>> discovered = getScenariosInPackage();
    Set<Class<? extends Scenario>> registered = getScenariosViaServiceLoader();

    assertThat(registered).containsExactlyInAnyOrderElementsOf(discovered);
  }

  /**
   * JUnit {@link ArgumentsProvider} that provides all registered
   * {@link Scenario} providers via Java's {@link ServiceLoader}
   * mechanism.
   */
  private static final class ScenariosArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) {
      return ServiceLoader.load(Scenario.class).stream()
        .map(ServiceLoader.Provider::get)
        .map(org.junit.jupiter.params.provider.Arguments::of);
    }
  }

  /**
   * Load all registered {@link Scenario} providers via Java's
   * {@link ServiceLoader} mechanism.
   *
   * @return set of registered scenario implementation classes
   */
  private static Set<Class<? extends Scenario>> getScenariosViaServiceLoader() {
    return ServiceLoader.load(Scenario.class).stream()
      .map(ServiceLoader.Provider::type)
      .map(ScenariosTest::asScenarioClass)
      .collect(Collectors.toSet());
  }

  /**
   * Discover all concrete implementations of {@link Scenario} in the
   * {@code com.coremedia.labs.translation.gcc.facade.mock.scenarios} package
   * that follow the naming convention (class name ends with "Scenario").
   * <p>
   * Uses the
   * <a href="https://github.com/classgraph/classgraph">award-winning ClassGraph library</a>
   * to scan the classpath.
   *
   * @return set of discovered scenario implementation classes
   */
  private static Set<Class<? extends Scenario>> getScenariosInPackage() {
    Set<Class<? extends Scenario>> discovered;
    try (ScanResult scan = new ClassGraph()
      .enableClassInfo()
      .acceptPackages(SCENARIOS_PACKAGE)
      .scan()) {

      discovered = scan.getClassesImplementing(Scenario.class.getName())
        .stream()
        .map(ClassInfo::loadClass)
        .filter(ScenariosTest::isRelevantScenario)
        .map(ScenariosTest::asScenarioClass)
        .collect(Collectors.toSet());
    }
    return discovered;
  }

  private static boolean isRelevantScenario(Class<?> scenarioClass) {
    return scenarioClass.getSimpleName().endsWith("Scenario")
      && !scenarioClass.isInterface()
      && !Modifier.isAbstract(scenarioClass.getModifiers());
  }

  @SuppressWarnings("unchecked")
  private static Class<? extends Scenario> asScenarioClass(Class<?> c) {
    return (Class<? extends Scenario>) c;
  }
}
