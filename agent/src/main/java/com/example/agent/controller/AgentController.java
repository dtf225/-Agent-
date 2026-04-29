package com.example.agent.controller;

import com.example.agent.agent.AgentOrchestrator;
import com.example.agent.agent.collection.LogCollectorAgent;
import com.example.agent.model.AnalysisResult;
import com.example.agent.model.LogEntry;
import com.example.agent.model.RepairSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentOrchestrator orchestrator;
    private final LogCollectorAgent logCollectorAgent;

    public AgentController(AgentOrchestrator orchestrator, LogCollectorAgent logCollectorAgent) {
        this.orchestrator = orchestrator;
        this.logCollectorAgent = logCollectorAgent;
    }

    @PostMapping("/simulate-error")
    public Map<String, Object> simulateError(@RequestParam(defaultValue = "NullPointerException") String type) {
        log.info("收到模拟异常请求: type={}", type);

        Map<String, Object> result = new HashMap<>();

        try {
            switch (type.toLowerCase()) {
                case "npe":
                case "nullpointer":
                case "nullpointerexception":
                    String nullStr = null;
                    try {
                        nullStr.length();
                    } catch (NullPointerException e) {
                        log.error("模拟空指针异常", e);
                        result.put("type", "NullPointerException");
                    }
                    break;

                case "timeout":
                    log.error("java.util.concurrent.TimeoutException: 连接超时: /192.168.1.100:3306",
                            new java.util.concurrent.TimeoutException("连接超时: /192.168.1.100:3306"));
                    break;

                case "sql":
                    log.error("org.springframework.dao.DataAccessException: 无法获取数据库连接",
                            new RuntimeException("Connection pool exhausted"));
                    break;

                case "illegal":
                    log.error("java.lang.IllegalArgumentException: 参数 'userId' 不能为 null",
                            new IllegalArgumentException("参数 'userId' 不能为 null"));
                    break;

                case "oom":
                    log.error("java.lang.OutOfMemoryError: Java heap space",
                            new OutOfMemoryError("Java heap space"));
                    break;

                case "connect":
                    log.error("java.net.ConnectException: Connection refused: connect to 10.0.0.50:8080",
                            new java.net.ConnectException("Connection refused: connect to 10.0.0.50:8080"));
                    break;

                case "class":
                    log.error("java.lang.ClassNotFoundException: com.example.missing.Dependency",
                            new ClassNotFoundException("com.example.missing.Dependency"));
                    break;

                case "bean":
                    log.error("org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'com.example.service.FooService' available",
                            new RuntimeException("NoSuchBeanDefinitionException: com.example.service.FooService"));
                    break;

                default:
                    log.error("模拟异常: " + type, new RuntimeException("模拟异常: " + type));
                    break;
            }

            result.put("success", true);
            result.put("message", "已触发 " + type + " 异常，多 Agent 协作管道正在运行");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "模拟异常失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/logs")
    public Map<String, Object> getLogs(@RequestParam(defaultValue = "50") int limit) {
        List<LogEntry> logs = logCollectorAgent.getCollectedLogs(limit);
        List<LogEntry> errorLogs = logCollectorAgent.getErrorLogs(limit);

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", logCollectorAgent.getTotalCount());
        result.put("errorCount", logCollectorAgent.getErrorCount());
        result.put("recentLogs", logs);
        result.put("recentErrors", errorLogs);
        return result;
    }

    @GetMapping("/analysis")
    public Map<String, Object> getAnalysis(@RequestParam(defaultValue = "20") int limit) {
        List<AnalysisResult> results = orchestrator.getAnalysisResults(limit);

        Map<String, Object> result = new HashMap<>();
        result.put("count", results.size());
        result.put("results", results);
        return result;
    }

    @GetMapping("/suggestions")
    public Map<String, Object> getSuggestions(@RequestParam(defaultValue = "20") int limit) {
        List<RepairSuggestion> suggestions = orchestrator.getRepairSuggestions(limit);

        Map<String, Object> result = new HashMap<>();
        result.put("count", suggestions.size());
        result.put("suggestions", suggestions);
        return result;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        dashboard.put("totalLogs", logCollectorAgent.getTotalCount());
        dashboard.put("errorLogs", logCollectorAgent.getErrorCount());
        dashboard.put("analysisResults", orchestrator.getAnalysisResults(100).size());
        dashboard.put("repairSuggestions", orchestrator.getRepairSuggestions(100).size());

        List<AnalysisResult> recentAnalysis = orchestrator.getAnalysisResults(5);
        dashboard.put("recentAnalysis", recentAnalysis);

        List<RepairSuggestion> recentSuggestions = orchestrator.getRepairSuggestions(5);
        dashboard.put("recentSuggestions", recentSuggestions);

        return dashboard;
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clear() {
        orchestrator.clearHistory();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "历史数据已清除");
        return result;
    }

    @PostMapping("/manual-analyze")
    public Map<String, Object> manualAnalyze(@RequestBody Map<String, String> body) {
        String level = body.getOrDefault("level", "ERROR");
        String message = body.getOrDefault("message", "手动分析请求");
        String stackTrace = body.getOrDefault("stackTrace", null);

        LogEntry entry = logCollectorAgent.collectAndParse(
                level,
                body.getOrDefault("logger", "manual"),
                message,
                body.getOrDefault("thread", "main"),
                stackTrace,
                null
        );

        AnalysisResult analysisResult = orchestrator.analyzeManually(entry);
        RepairSuggestion suggestion = orchestrator.suggestFix(analysisResult);
        orchestrator.sendNotification(suggestion);

        Map<String, Object> result = new HashMap<>();
        result.put("logEntry", entry);
        result.put("analysis", analysisResult);
        result.put("suggestion", suggestion);
        result.put("notificationSent", suggestion.isNotificationSent());
        return result;
    }
}
