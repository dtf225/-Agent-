package com.example.agent.agent.collection;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.example.agent.model.LogEntry;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class AgentLogbackAppender extends AppenderBase<ILoggingEvent> {

    private static LogCollectorAgent logCollectorAgent;

    public static void setLogCollectorAgent(LogCollectorAgent agent) {
        logCollectorAgent = agent;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (logCollectorAgent == null) {
            return;
        }

        String level = event.getLevel().toString();
        String loggerName = event.getLoggerName();
        String message = event.getFormattedMessage();
        String threadName = event.getThreadName();

        String stackTrace = null;
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            stackTrace = ThrowableProxyUtil.asString(throwableProxy);
        }

        Map<String, String> context = new HashMap<>();
        Map<String, String> mdcMap = event.getMDCPropertyMap();
        if (mdcMap != null) {
            context.putAll(mdcMap);
        }
        context.put("serviceName", "agent-ops-service");

        if (event.getTimeStamp() > 0) {
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(event.getTimeStamp()), ZoneId.systemDefault());
            context.put("logTimestamp", timestamp.toString());
        }

        logCollectorAgent.collectAndParse(level, loggerName, message, threadName, stackTrace, context);
    }
}
