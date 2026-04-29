package com.example.agent.config;

import com.example.agent.agent.collection.LogCollectorAgent;
import com.example.agent.agent.collection.AgentLogbackAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AgentConfig {

    private final LogCollectorAgent logCollectorAgent;

    public AgentConfig(LogCollectorAgent logCollectorAgent) {
        this.logCollectorAgent = logCollectorAgent;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerLogbackAppender() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        AgentLogbackAppender.setLogCollectorAgent(logCollectorAgent);

        AgentLogbackAppender appender = new AgentLogbackAppender();
        appender.setContext(loggerContext);
        appender.setName("AGENT_LOG_APPENDER");
        appender.start();

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(appender);

        org.slf4j.Logger log = LoggerFactory.getLogger(AgentConfig.class);
        log.info("AgentLogbackAppender 已注册到 Logback 根 Logger，Agent 日志采集管道就绪");
    }
}
