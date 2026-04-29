package com.example.agent.agent.collection;

import com.example.agent.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class LogCollectorAgent {

    private static final Logger log = LoggerFactory.getLogger(LogCollectorAgent.class);

    private final List<LogEntry> collectedLogs = new CopyOnWriteArrayList<>();
    private final Map<String, Consumer<LogEntry>> errorListeners = new ConcurrentHashMap<>();

    public LogEntry collectAndParse(String level, String loggerName, String message,
                                     String threadName, String stackTrace, Map<String, String> context) {
        LogEntry entry = new LogEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTimestamp(LocalDateTime.now());
        entry.setLevel(level);
        entry.setLogger(loggerName);
        entry.setMessage(message);
        entry.setThreadName(threadName);
        entry.setStackTrace(stackTrace);
        entry.setServiceName(context != null ? context.getOrDefault("serviceName", "unknown") : "unknown");
        entry.setContext(context);
        entry.setHasException(stackTrace != null && !stackTrace.isEmpty());

        collectedLogs.add(entry);

        log.debug("[采集Agent] 结构化解析日志: level={}, logger={}, hasException={}",
                level, loggerName, entry.isHasException());

        if (entry.isError() && entry.isHasException()) {
            log.warn("[采集Agent] 捕获 ERROR 级别异常日志, 触发分析管道: id={}", entry.getId());
            notifyErrorListeners(entry);
        }

        return entry;
    }

    public String registerErrorListener(String listenerId, Consumer<LogEntry> listener) {
        errorListeners.put(listenerId, listener);
        log.info("[采集Agent] 注册错误监听器: {}", listenerId);
        return listenerId;
    }

    public void unregisterErrorListener(String listenerId) {
        errorListeners.remove(listenerId);
        log.info("[采集Agent] 注销错误监听器: {}", listenerId);
    }

    private void notifyErrorListeners(LogEntry errorEntry) {
        for (Map.Entry<String, Consumer<LogEntry>> listener : errorListeners.entrySet()) {
            try {
                listener.getValue().accept(errorEntry);
            } catch (Exception e) {
                log.error("[采集Agent] 通知监听器 {} 失败", listener.getKey(), e);
            }
        }
    }

    public List<LogEntry> getCollectedLogs(int limit) {
        int size = collectedLogs.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(collectedLogs.subList(fromIndex, size));
    }

    public List<LogEntry> getErrorLogs(int limit) {
        return collectedLogs.stream()
                .filter(LogEntry::isError)
                .skip(Math.max(0, collectedLogs.stream().filter(LogEntry::isError).count() - limit))
                .toList();
    }

    public void clear() {
        collectedLogs.clear();
    }

    public int getTotalCount() {
        return collectedLogs.size();
    }

    public long getErrorCount() {
        return collectedLogs.stream().filter(LogEntry::isError).count();
    }
}
