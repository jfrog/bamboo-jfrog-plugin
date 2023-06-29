package org.jfrog.bamboo.utils;

import com.atlassian.bamboo.build.logger.BuildLogger;
import org.apache.logging.log4j.Logger;

/**
 * Wrapper for Bamboo build logger, records log messages from BuildInfo
 */
public class BuildLog implements org.jfrog.build.api.util.Log {
    private final Logger log;
    private BuildLogger buildLogger;
    private final String JFROG_PREFIX = "[JFrog Plugin] ";

    public BuildLog(Logger log) {
        this.log = log;
    }

    public BuildLog(Logger log, BuildLogger buildLogger) {
        this.log = log;
        this.buildLogger = buildLogger;
    }

    private String addPrefix(String message) {
        return JFROG_PREFIX + message;
    }

    public void debug(String message) {
        message = addPrefix(message);
        log.debug(message);
    }

    public void info(String message) {
        message = addPrefix(message);
        if (this.buildLogger != null) {
            this.buildLogger.addBuildLogEntry(message);
        }
        log.info(message);
    }

    public void warn(String message) {
        message = addPrefix(message);
        if (this.buildLogger != null) {
            this.buildLogger.addBuildLogEntry(message);
        }
        log.warn(message);
    }

    public void error(String message) {
        message = addPrefix(message);
        if (this.buildLogger != null) {
            this.buildLogger.addErrorLogEntry(message);
        }
        log.error(message);
    }

    public void error(String message, Throwable e) {
        message = addPrefix(message);
        if (this.buildLogger != null) {
            this.buildLogger.addErrorLogEntry(message, e);
        }
        log.error(message, e);
    }
}