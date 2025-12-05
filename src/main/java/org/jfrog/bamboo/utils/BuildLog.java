package org.jfrog.bamboo.utils;

import com.atlassian.bamboo.build.logger.BuildLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.lang.String.format;

/**
 * Wrapper for Bamboo build logger, records log messages from BuildInfo.
 */
public class BuildLog implements org.jfrog.build.api.util.Log {
    private static final String JFROG_PREFIX = "JFrog Plugin";
    private final Logger log = LogManager.getLogger(JFROG_PREFIX);
    private final BuildLogger buildLogger;

    public BuildLog() {
        buildLogger = null;
    }

    public BuildLog(BuildLogger buildLogger) {
        this.buildLogger = buildLogger;
    }

    private String addPrefix(String message) {
        return format("[%s] %s", JFROG_PREFIX, message);
    }

    public void debug(String message) {
        log.debug(message);
    }

    public void info(String message) {
        if (buildLogger != null) {
            buildLogger.addBuildLogEntry(addPrefix(message));
        }
        log.info(message);
    }

    public void warn(String message) {
        if (buildLogger != null) {
            buildLogger.addBuildLogEntry(addPrefix(message));
        }
        log.warn(message);
    }

    public void error(String message) {
        String prefixedMessage = addPrefix(message);
        if (buildLogger != null) {
            buildLogger.addErrorLogEntry(addPrefix(message));
        }
        log.error(prefixedMessage);
    }

    @Override
    public void error(String message, Throwable throwable) {
        String prefixedMessage = addPrefix(message);
        if (buildLogger != null) {
            // Sanitize error message to avoid exposing internal structure
            String sanitizedMessage = throwable != null ? "An error occurred during execution" : prefixedMessage;
            buildLogger.addErrorLogEntry(sanitizedMessage);
        }
        // Log error without exposing internal details
        log.error(prefixedMessage);
    }
}
