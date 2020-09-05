package com.samourai.soroban.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogbackUtils {
  public static final String LOGBACK_CLASSIC = "ch.qos.logback.classic";
  public static final String LOGBACK_CLASSIC_LOGGER = "ch.qos.logback.classic.Logger";
  public static final String LOGBACK_CLASSIC_LEVEL = "ch.qos.logback.classic.Level";
  private static final Logger logger = LoggerFactory.getLogger(LogbackUtils.class);

  private LogbackUtils() {}

  public static boolean setLogLevel(String loggerName, String logLevel) {
    String logLevelUpper = logLevel == null ? "OFF" : logLevel.toUpperCase();

    try {
      Package logbackPackage = Package.getPackage("ch.qos.logback.classic");
      if (logbackPackage == null) {
        logger.info("Logback is not in the classpath!");
        return false;
      } else {
        if (loggerName == null || loggerName.trim().isEmpty()) {
          loggerName = (String) getFieldValue("ch.qos.logback.classic.Logger", "ROOT_LOGGER_NAME");
        }

        Logger loggerObtained = LoggerFactory.getLogger(loggerName);
        if (loggerObtained == null) {
          logger.warn("No logger for the name: {}", loggerName);
          return false;
        } else {
          Object logLevelObj = getFieldValue("ch.qos.logback.classic.Level", logLevelUpper);
          if (logLevelObj == null) {
            logger.warn("No such log level: {}", logLevelUpper);
            return false;
          } else {
            Class<?>[] paramTypes = new Class[] {logLevelObj.getClass()};
            Object[] params = new Object[] {logLevelObj};
            Class<?> clz = Class.forName("ch.qos.logback.classic.Logger");
            Method method = clz.getMethod("setLevel", paramTypes);
            method.invoke(loggerObtained, params);
            logger.debug("Log level set to {} for the logger '{}'", logLevelUpper, loggerName);
            return true;
          }
        }
      }
    } catch (Exception var10) {
      logger.warn(
          "Couldn't set log level to {} for the logger '{}'",
          new Object[] {logLevelUpper, loggerName, var10});
      return false;
    }
  }

  private static Object getFieldValue(String fullClassName, String fieldName) {
    try {
      Class<?> clazz = Class.forName(fullClassName);
      Field field = clazz.getField(fieldName);
      return field.get((Object) null);
    } catch (Exception var4) {
      return null;
    }
  }
}
