package com.coremedia.labs.translation.gcc.facade.def;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Handler for JUL Logger required for
 * {@link org.gs4tr.gcc.restclient.GCConfig#setLogger(java.util.logging.Logger)}.
 */
final class SLF4JHandler extends Handler {
  @NonNull
  private final Logger slf4jLogger;

  private SLF4JHandler(String loggerName) {
    slf4jLogger = LoggerFactory.getLogger(loggerName);
    setLevel(Level.ALL);
    setFilter(null);
  }

  @NonNull
  public static java.util.logging.Logger getLogger(@NonNull String name) {
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
    logger.setLevel(Level.ALL);
    logger.setUseParentHandlers(false);
    logger.addHandler(new SLF4JHandler(name));
    return logger;
  }

  @NonNull
  public static java.util.logging.Logger getLogger(@NonNull Class<?> clazz) {
    return getLogger(clazz.getName());
  }

  @Override
  public void publish(@NonNull LogRecord record) {
    Level level = record.getLevel();
    String message = record.getMessage();
    Throwable thrown = record.getThrown();

    if (level == Level.SEVERE) {
      slf4jLogger.error(message, thrown);
    } else if (level == Level.WARNING) {
      slf4jLogger.warn(message, thrown);
    } else if (level == Level.INFO) {
      slf4jLogger.info(message, thrown);
    } else if (level == Level.CONFIG || level == Level.FINE) {
      slf4jLogger.debug(message, thrown);
    } else {
      slf4jLogger.trace(message, thrown);
    }
  }

  @Override
  public void flush() {
    // No-op
  }

  @Override
  public void close() {
    // No-op
  }
}
