package com.coremedia.labs.translation.gcc.facade.def;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Handler for JUL Logger required for
 * {@link org.gs4tr.gcc.restclient.GCConfig#setLogger(java.util.logging.Logger)}.
 *
 * @since 2406.1
 */
@NullMarked
final class SLF4JHandler extends Handler {
  private final Logger slf4jLogger;

  private SLF4JHandler(String loggerName) {
    slf4jLogger = LoggerFactory.getLogger(loggerName);
    setLevel(Level.ALL);
    setFilter(null);
  }

  public static java.util.logging.Logger getLogger(String name) {
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
    logger.setLevel(Level.ALL);
    logger.setUseParentHandlers(false);
    logger.addHandler(new SLF4JHandler(name));
    return logger;
  }

  public static java.util.logging.Logger getLogger(Class<?> clazz) {
    return getLogger(clazz.getName());
  }

  @Override
  public void publish(LogRecord record) {
    Level level = record.getLevel();
    String message = record.getMessage();
    Throwable thrown = record.getThrown();
    String messageWithLevel = "%s: %s".formatted(level.getName(), message);
    if (level == Level.SEVERE) {
      slf4jLogger.error(messageWithLevel, thrown);
    } else if (level == Level.WARNING) {
      slf4jLogger.warn(messageWithLevel, thrown);
    } else if (level == Level.INFO) {
      // Lower to `debug` because `info` for GCFacade is too verbose.
      slf4jLogger.debug(messageWithLevel, thrown);
    } else if (level == Level.CONFIG || level == Level.FINE) {
      slf4jLogger.debug(messageWithLevel, thrown);
    } else {
      slf4jLogger.trace(messageWithLevel, thrown);
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
