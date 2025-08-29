package com.coremedia.labs.translation.gcc.workflow;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A source for settings, such as provided by Spring Context, settings
 * read from content items, etc.
 */
@FunctionalInterface
public interface SettingsSource extends Supplier<Map<String, Object>> {
}
