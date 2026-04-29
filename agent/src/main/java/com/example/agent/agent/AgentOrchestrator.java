package com.example.agent.agent;

import com.example.agent.agent.analysis.AnalysisAgent;
import com.example.agent.agent.collection.LogCollectorAgent;
import com.example.agent.agent.repair.NotificationService;
import com.example.agent.agent.repair.RepairSuggestionAgent;
import com.example.agent.model.AnalysisResult;
import com.example.agent.model.LogEntry;
import com.example.agent.model.RepairSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final LogCollectorAgent logCollectorAgent;
    private final AnalysisAgent analysisAgent;
    private final RepairSuggestionAgent repairSuggestionAgent;
    private final NotificationService notificationService;

    private final List<AnalysisResult> analysisResults = new ArrayList<>();
    private final List<RepairSuggestion> repairSuggestions = new ArrayList<>();

    public AgentOrchestrator(LogCollectorAgent logCollectorAgent,
                             AnalysisAgent analysisAgent,
                             RepairSuggestionAgent repairSuggestionAgent,
                             NotificationService notificationService) {
        this.logCollectorAgent = logCollectorAgent;
        this.analysisAgent = analysisAgent;
        this.repairSuggestionAgent = repairSuggestionAgent;
        this.notificationService = notificationService;

        initErrorPipeline();
    }

    private void initErrorPipeline() {
        logCollectorAgent.registerErrorListener("analysis-pipeline", this::handleErrorLog);
        log.info("[编排器] 多 Agent 协作管道已初始化: 采集Agent -> 分析Agent -> 修复建议Agent -> 通知服务");
    }

    private void handleErrorLog(LogEntry errorEntry) {
        log.info("[编排器] ========== 多 Agent 协作管道触发 ==========");

        try {
            log.info("[编排器] Step 1/4: 采集Agent 已捕获 ERROR 日志: id={}, message={}",
                    errorEntry.getId(), errorEntry.getMessage());

            log.info("[编排器] Step 2/4: 启动分析Agent 进行长链推理...");
            AnalysisResult analysisResult = analysisAgent.analyze(errorEntry);
            analysisResults.add(analysisResult);

            log.info("[编排器] 分析完成: rootCause={}, confidence={}",
                    analysisResult.getRootCauseCategory(),
                    String.format("%.2f", analysisResult.getConfidenceScore()));

            log.info("[编排器] Step 3/4: 启动修复建议Agent 生成修复方案...");
            RepairSuggestion suggestion = repairSuggestionAgent.generate(analysisResult);
            repairSuggestions.add(suggestion);

            log.info("[编排器] 修复建议生成: title={}, severity={}",
                    suggestion.getTitle(), suggestion.getSeverity());

            log.info("[编排器] Step 4/4: 推送修复建议到钉钉/飞书...");
            notificationService.sendNotification(suggestion);

            log.info("[编排器] ========== 多 Agent 协作管道完成 ==========");

        } catch (Exception e) {
            log.error("[编排器] 多 Agent 协作管道执行异常", e);
        }
    }

    public AnalysisResult analyzeManually(LogEntry logEntry) {
        log.info("[编排器] 手动触发分析管道");
        AnalysisResult result = analysisAgent.analyze(logEntry);
        analysisResults.add(result);
        return result;
    }

    public RepairSuggestion suggestFix(AnalysisResult analysisResult) {
        RepairSuggestion suggestion = repairSuggestionAgent.generate(analysisResult);
        repairSuggestions.add(suggestion);
        return suggestion;
    }

    public void sendNotification(RepairSuggestion suggestion) {
        notificationService.sendNotification(suggestion);
    }

    public List<AnalysisResult> getAnalysisResults(int limit) {
        int size = analysisResults.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(analysisResults.subList(fromIndex, size));
    }

    public List<RepairSuggestion> getRepairSuggestions(int limit) {
        int size = repairSuggestions.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(repairSuggestions.subList(fromIndex, size));
    }

    public void clearHistory() {
        analysisResults.clear();
        repairSuggestions.clear();
        logCollectorAgent.clear();
    }
}
