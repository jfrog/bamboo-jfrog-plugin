package org.jfrog.bamboo.utils;

import com.atlassian.bamboo.build.logger.BuildLogger;
import org.apache.logging.log4j.Logger;

/**
 * Wrapper for Bamboo build logger, records log messages from BuildInfo.
 */
public class BuildLog implements org.jfrog.build.api.util.Log {
    private static final String JFROG_PREFIX = "[JFrog Plugin] ";

    private final Logger log;
    private final BuildLogger buildLogger;

    public BuildLog(Logger log) {
        this(log, null);
    }

    public BuildLog(Logger log, BuildLogger buildLogger) {
        this.log = log;
        this.buildLogger = buildLogger;
    }

    private String addPrefix(String message) {
        return JFROG_PREFIX + message;
    }

    public void debug(String message) {
        log.debug(addPrefix(message));
    }

    public void info(String message) {
        String prefixedMessage = addPrefix(message);
        if (buildLogger != null) {
            buildLogger.addBuildLogEntry(prefixedMessage);
        }
        log.info(prefixedMessage);
    }

    public void warn(String message) {
        String prefixedMessage = addPrefix(message);
        if (buildLogger != null) {
            buildLogger.addBuildLogEntry(prefixedMessage);
        }
        log.warn(prefixedMessage);
    }

    public void error(String message) {
        String prefixedMessage = addPrefix(message);
        if (buildLogger != null) {
            buildLogger.addErrorLogEntry(prefixedMessage);
        }
        log.error(prefixedMessage);
    }

    public void error(String message, Throwable e) {
        String prefixedMessage = addPrefix(message);
        if (buildLogger != null) {
            buildLogger.addErrorLogEntry(prefixedMessage, e);
        }
        log.error(prefixedMessage, e);
    }
}
