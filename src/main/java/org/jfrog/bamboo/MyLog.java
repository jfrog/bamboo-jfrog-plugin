package org.jfrog.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import org.apache.logging.log4j.Logger;
import org.jfrog.build.api.util.Log;

import java.util.logging.Level;

/**
 * Wrapper for Jenkins build logger, records log messages from BuildInfo
 */
public class MyLog implements Log {
    private final Logger log;
    private final BuildLogger buildLogger;
    private final String JFROG_PREFIX = "[JFrog Plugin] ";

    public MyLog(Logger log, BuildLogger buildLogger) {
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