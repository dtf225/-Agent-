package com.example.agent.agent.analysis;

import com.example.agent.agent.collection.LogCollectorAgent;
import com.example.agent.model.AnalysisResult;
import com.example.agent.model.AnalysisResult.ReasoningStep;
import com.example.agent.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AnalysisAgent {

    private static final Logger log = LoggerFactory.getLogger(AnalysisAgent.class);

    private static final Pattern STACK_TRACE_LINE_PATTERN =
            Pattern.compile("\\s+at\\s+([\\w.$]+)\\.([\\w<>$]+)\\(([\\w.]+):(\\d+)\\)");

    private static final Pattern CAUSED_BY_PATTERN =
            Pattern.compile("Caused by:\\s+([\\w.$]+):\\s*(.+)");

    private final LogCollectorAgent logCollectorAgent;

    public AnalysisAgent(LogCollectorAgent logCollectorAgent) {
        this.logCollectorAgent = logCollectorAgent;
    }

    public AnalysisResult analyze(LogEntry errorLogEntry) {
        log.info("[分析Agent] 开始长链推理分析: logEntryId={}", errorLogEntry.getId());

        AnalysisResult result = new AnalysisResult();
        result.setId(UUID.randomUUID().toString());
        result.setLogEntryId(errorLogEntry.getId());
        result.setAnalysisTime(LocalDateTime.now());

        ReasoningStep step1 = parseStackTrace(errorLogEntry, result);
        result.getReasoningChain().add(step1);

        ReasoningStep step2 = contextAnalysis(errorLogEntry, result);
        result.getReasoningChain().add(step2);

        ReasoningStep step3 = rootCauseReasoning(errorLogEntry, result);
        result.getReasoningChain().add(step3);

        ReasoningStep step4 = finalDiagnosis(result);
        result.getReasoningChain().add(step4);

        result.setConfidenceScore(calculateConfidence(result));

        log.info("[分析Agent] 长链推理完成: exceptionType={}, rootCause={}, confidence={}",
                result.getExceptionType(), result.getRootCauseCategory(),
                String.format("%.2f", result.getConfidenceScore()));

        return result;
    }

    private ReasoningStep parseStackTrace(LogEntry entry, AnalysisResult result) {
        String stackTrace = entry.getStackTrace();
        ReasoningStep step = new ReasoningStep();
        step.setStepOrder(1);
        step.setStepName("堆栈信息提取");

        if (stackTrace == null || stackTrace.isEmpty()) {
            step.setObservation("日志中无堆栈信息");
            step.setDeduction("可能是手动记录的 ERROR 日志，非异常抛出");
            step.setConclusion("无法提取异常类型，将基于日志消息进行分析");
            result.setExceptionType("ManualError");
            result.setExceptionMessage(entry.getMessage());
            return step;
        }

        String[] lines = stackTrace.split("\n");
        String firstLine = lines[0].trim();

        if (firstLine.contains(":")) {
            int colonIdx = firstLine.indexOf(':');
            String exceptionName = firstLine.substring(0, colonIdx).trim();
            String exceptionMessage = firstLine.substring(colonIdx + 1).trim();
            result.setExceptionType(exceptionName);
            result.setExceptionMessage(exceptionMessage);
        } else {
            result.setExceptionType(firstLine);
            result.setExceptionMessage("");
        }

        for (String line : lines) {
            Matcher m = STACK_TRACE_LINE_PATTERN.matcher(line);
            if (m.find()) {
                result.setFailingClass(m.group(1));
                result.setFailingMethod(m.group(2));
                try {
                    result.setFailingLine(Integer.parseInt(m.group(4)));
                } catch (NumberFormatException ignored) {
                }
                break;
            }
        }

        for (String line : lines) {
            Matcher m = CAUSED_BY_PATTERN.matcher(line);
            if (m.find()) {
                step.setObservation("发现嵌套异常 Caused by: " + m.group(1) + " - " + m.group(2));
                step.setDeduction("存在多层异常链，根本原因可能在底层");
                step.setConclusion("异常类型: " + result.getExceptionType() +
                        ", 发生位置: " + result.getFailingClass() + "." +
                        result.getFailingMethod() + ":" + result.getFailingLine());
                return step;
            }
        }

        step.setObservation("堆栈异常: " + result.getExceptionType());
        step.setDeduction("堆栈顶层指明异常直接发生位置");
        step.setConclusion("异常类型: " + result.getExceptionType() +
                ", 发生位置: " + result.getFailingClass() + "." +
                result.getFailingMethod() + ":" + result.getFailingLine());

        return step;
    }

    private ReasoningStep contextAnalysis(LogEntry entry, AnalysisResult result) {
        ReasoningStep step = new ReasoningStep();
        step.setStepOrder(2);
        step.setStepName("上下文关联分析");

        StringBuilder obs = new StringBuilder();
        obs.append("日志级别: ").append(entry.getLevel()).append(", ");
        obs.append("Logger: ").append(entry.getLogger()).append(", ");
        obs.append("线程: ").append(entry.getThreadName()).append(", ");
        obs.append("消息: ").append(entry.getMessage());

        if (entry.getContext() != null && !entry.getContext().isEmpty()) {
            obs.append(", 上下文数据: ").append(entry.getContext());
        }

        step.setObservation(obs.toString());

        String exType = result.getExceptionType();
        if (exType != null) {
            if (exType.contains("NullPointerException")) {
                step.setDeduction("空指针异常通常由未初始化的对象或返回 null 的方法调用引起");
            } else if (exType.contains("Timeout") || exType.contains("TimeoutException")) {
                step.setDeduction("超时异常表明外部服务或资源响应过慢，需排查网络、数据库或第三方服务");
            } else if (exType.contains("SQL") || exType.contains("DataAccess")) {
                step.setDeduction("数据访问异常与 SQL 语句、数据库连接或事务管理有关");
            } else if (exType.contains("IllegalArgument") || exType.contains("IllegalState")) {
                step.setDeduction("参数/状态异常表明业务逻辑校验未通过或对象状态不正确");
            } else if (exType.contains("OutOfMemory")) {
                step.setDeduction("内存溢出通常由内存泄漏、大对象分配或堆空间不足引起");
            } else {
                step.setDeduction("异常需结合业务上下文进一步排查");
            }
        } else {
            step.setDeduction("无法从异常类型推断上下文");
        }

        step.setConclusion("上下文分析完成，进入根因推理阶段");
        return step;
    }

    private ReasoningStep rootCauseReasoning(LogEntry entry, AnalysisResult result) {
        ReasoningStep step = new ReasoningStep();
        step.setStepOrder(3);
        step.setStepName("根因推理");

        String exType = result.getExceptionType();
        String exMsg = result.getExceptionMessage();

        if (exType == null) {
            result.setRootCauseCategory("UNKNOWN");
            result.setRootCauseDescription("无法确定根因，建议人工排查");
            step.setConclusion("根因不明确");
            return step;
        }

        if (exType.contains("NullPointerException")) {
            result.setRootCauseCategory("空指针异常");
            result.setRootCauseDescription("在 " + result.getFailingClass() + "." +
                    result.getFailingMethod() + " 中，某对象为 null 却被调用了方法或访问了属性。" +
                    "请检查该方法中的对象初始化逻辑及上游调用链的返回值。");

        } else if (exType.contains("Timeout") || exType.contains("TimeoutException")) {
            result.setRootCauseCategory("超时异常");
            result.setRootCauseDescription("服务调用在 " + result.getFailingClass() + " 中发生超时。" +
                    "可能原因：(1) 数据库连接池耗尽 (2) 网络延迟过高 (3) 下游服务响应慢 " +
                    "(4) 未设置合理的超时时间。建议检查连接池配置及下游服务健康状态。");

        } else if (exType.contains("SQLException") || exType.contains("DataAccessException") ||
                exType.contains("JdbcSQL")) {
            result.setRootCauseCategory("数据访问异常");
            result.setRootCauseDescription("数据库操作在 " + result.getFailingClass() + " 中失败。" +
                    "可能原因：(1) SQL 语法错误 (2) 数据库连接失败 (3) 事务超时 (4) 数据约束冲突。" +
                    "请检查 SQL 日志及数据库连接状态。");

        } else if (exType.contains("IllegalArgumentException")) {
            result.setRootCauseCategory("参数校验异常");
            result.setRootCauseDescription("在 " + result.getFailingClass() + "." +
                    result.getFailingMethod() + " 中收到了非法参数。" +
                    "建议添加参数校验逻辑或修复调用方传参。");

        } else if (exType.contains("IllegalStateException")) {
            result.setRootCauseCategory("状态异常");
            result.setRootCauseDescription("在 " + result.getFailingClass() + "." +
                    result.getFailingMethod() + " 中对象状态不符合预期。" +
                    "请检查 Bean 生命周期、初始化顺序或并发状态管理。");

        } else if (exType.contains("OutOfMemoryError")) {
            result.setRootCauseCategory("内存溢出");
            result.setRootCauseDescription("JVM 堆内存不足。建议：(1) 分析 heap dump (2) 检查是否有内存泄漏 " +
                    "(3) 增大 -Xmx 参数 (4) 检查大对象分配。");

        } else if (exType.contains("ConnectException") || exType.contains("HttpHostConnect")) {
            result.setRootCauseCategory("网络连接异常");
            result.setRootCauseDescription("无法连接到目标服务。" +
                    "请检查目标服务是否运行、网络策略及防火墙配置。");

        } else if (exType.contains("ClassNotFoundException") || exType.contains("NoClassDefFoundError")) {
            result.setRootCauseCategory("类加载异常");
            result.setRootCauseDescription("缺少必要的类或依赖。" +
                    "请检查 Maven/Gradle 依赖是否完整，是否存在版本冲突。");

        } else if (exType.contains("NoSuchBeanDefinitionException")) {
            result.setRootCauseCategory("Spring Bean 缺失");
            result.setRootCauseDescription("Spring 容器中缺少必要的 Bean。" +
                    "请检查 @Component/@Service/@Repository 注解或 @Bean 配置是否正确。");

        } else {
            result.setRootCauseCategory("其他异常 - " + exType);
            result.setRootCauseDescription("异常类型: " + exType + "，消息: " + exMsg +
                    "。该异常未被归类到已知模式，建议结合具体异常信息排查。");
        }

        String msg = result.getExceptionMessage();
        if (msg != null && msg.length() > 200) {
            msg = msg.substring(0, 200) + "...";
        }

        step.setObservation("异常类型: " + exType + ", 异常消息: " + msg);
        step.setDeduction("根据异常类型 " + exType + " 匹配到已知根因模式");
        step.setConclusion("根因分类: " + result.getRootCauseCategory());

        return step;
    }

    private ReasoningStep finalDiagnosis(AnalysisResult result) {
        ReasoningStep step = new ReasoningStep();
        step.setStepOrder(4);
        step.setStepName("最终诊断");

        step.setObservation("经过堆栈提取、上下文分析和根因推理三个阶段的链式分析");
        step.setDeduction("综合各阶段结论，形成最终诊断");
        step.setConclusion("【诊断结论】" + result.getRootCauseDescription() +
                " | 置信度: " + String.format("%.1f%%", result.getConfidenceScore() * 100));

        return step;
    }

    private double calculateConfidence(AnalysisResult result) {
        double confidence = 0.5;

        if (result.getExceptionType() != null && !result.getExceptionType().equals("ManualError")) {
            confidence += 0.15;
        }
        if (result.getFailingClass() != null) {
            confidence += 0.10;
        }
        if (result.getFailingLine() > 0) {
            confidence += 0.10;
        }
        if (result.getRootCauseCategory() != null && !result.getRootCauseCategory().equals("UNKNOWN")) {
            confidence += 0.15;
        }

        return Math.min(confidence, 1.0);
    }
}
