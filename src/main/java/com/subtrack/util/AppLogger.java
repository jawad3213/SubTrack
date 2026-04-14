package com.subtrack.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppLogger {

    private final Logger logger;

    private AppLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    private AppLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static AppLogger getLogger(Class<?> clazz) {
        return new AppLogger(clazz);
    }

    public static AppLogger getLogger(String name) {
        return new AppLogger(name);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void debug(String template, Object... args) {
        logger.debug(template, args);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void info(String template, Object... args) {
        logger.info(template, args);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String template, Object... args) {
        logger.warn(template, args);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    public void error(String template, Throwable t, Object... args) {
        logger.error(template, t, args);
    }

    public void trace(String message) {
        logger.trace(message);
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public void logInfo(String message) {
        logger.info(message);
    }

    public void logError(String message) {
        logger.error(message);
    }

    public void logError(String message, Throwable t) {
        logger.error(message, t);
    }

    public void logWarn(String message) {
        logger.warn(message);
    }
}