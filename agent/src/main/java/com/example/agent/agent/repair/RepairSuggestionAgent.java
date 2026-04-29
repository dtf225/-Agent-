package com.example.agent.agent.repair;

import com.example.agent.model.AnalysisResult;
import com.example.agent.model.RepairSuggestion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RepairSuggestionAgent {

    private static final Logger log = LoggerFactory.getLogger(RepairSuggestionAgent.class);

    private static final Map<String, SuggestionTemplate> SUGGESTION_REGISTRY = new HashMap<>();

    static {
        SUGGESTION_REGISTRY.put("空指针异常", new SuggestionTemplate(
                "空指针异常修复",
                "CRITICAL",
                "NULL_CHECK",
                "## 空指针异常修复方案\n\n" +
                "### 1. 定位问题\n" +
                "找到可能返回 null 的调用链，检查对象是否被正确初始化。\n\n" +
                "### 2. 推荐修复方式\n" +
                "```java\n" +
                "// 方式一：使用 Optional 包装\n" +
                "Optional.ofNullable(object).ifPresent(o -> o.doSomething());\n\n" +
                "// 方式二：显式 null 检查\n" +
                "if (object != null) {\n" +
                "    object.doSomething();\n" +
                "} else {\n" +
                "    log.warn(\"对象为 null，跳过处理\");\n" +
                "    return defaultValue;\n" +
                "}\n\n" +
                "// 方式三：使用 @NonNull 注解 + Objects.requireNonNull\n" +
                "Objects.requireNonNull(object, \"参数不能为 null\");\n" +
                "```\n\n" +
                "### 3. 预防措施\n" +
                "- 方法参数添加 @Nullable/@NonNull 注解\n" +
                "- 避免方法返回 null，改用 Optional 或空集合\n" +
                "- 开启 IDE 的 null 检查警告\n",
                "https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html"
        ));

        SUGGESTION_REGISTRY.put("超时异常", new SuggestionTemplate(
                "超时异常修复",
                "HIGH",
                "TIMEOUT_CONFIG",
                "## 超时异常修复方案\n\n" +
                "### 1. 排查方向\n" +
                "检查数据库连接池、HTTP 客户端、消息队列的超时配置。\n\n" +
                "### 2. 推荐修复\n" +
                "```yaml\n" +
                "# application.yml 配置建议\n" +
                "spring:\n" +
                "  datasource:\n" +
                "    hikari:\n" +
                "      connection-timeout: 30000\n" +
                "      maximum-pool-size: 20\n" +
                "      minimum-idle: 5\n" +
                "\n" +
                "server:\n" +
                "  tomcat:\n" +
                "    connection-timeout: 60000\n" +
                "```\n\n" +
                "```java\n" +
                "// RestTemplate 超时设置\n" +
                "RestTemplate restTemplate = new RestTemplate();\n" +
                "HttpComponentsClientHttpRequestFactory factory = \n" +
                "    new HttpComponentsClientHttpRequestFactory();\n" +
                "factory.setConnectTimeout(5000);\n" +
                "factory.setReadTimeout(30000);\n" +
                "```\n\n" +
                "### 3. 熔断保护\n" +
                "建议引入 Resilience4j 或 Sentinel 实现熔断降级。\n",
                "https://resilience4j.readme.io/docs/circuitbreaker"
        ));

        SUGGESTION_REGISTRY.put("数据访问异常", new SuggestionTemplate(
                "数据访问异常修复",
                "HIGH",
                "DB_FIX",
                "## 数据访问异常修复方案\n\n" +
                "### 1. 检查 SQL\n" +
                "- 确认 SQL 语法正确\n" +
                "- 检查表名和字段名拼写\n" +
                "- 验证参数绑定\n\n" +
                "### 2. 连接排查\n" +
                "```java\n" +
                "// 检查数据库连接\n" +
                "@Autowired\n" +
                "private DataSource dataSource;\n\n" +
                "public void checkConnection() {\n" +
                "    try (Connection conn = dataSource.getConnection()) {\n" +
                "        log.info(\"数据库连接正常: {}\", conn.getMetaData().getURL());\n" +
                "    }\n" +
                "}\n" +
                "```\n\n" +
                "### 3. 事务管理\n" +
                "- 确认 @Transactional 注解配置正确\n" +
                "- 检查事务传播行为是否合适\n" +
                "- 避免在事务中进行耗时操作\n",
                "https://docs.spring.io/spring-framework/reference/data-access.html"
        ));

        SUGGESTION_REGISTRY.put("参数校验异常", new SuggestionTemplate(
                "参数校验异常修复",
                "MEDIUM",
                "VALIDATION",
                "## 参数校验异常修复\n\n" +
                "### 推荐方案\n" +
                "```java\n" +
                "@PostMapping(\"/api/data\")\n" +
                "public Result create(@Valid @RequestBody CreateRequest req) {\n" +
                "    // 使用 @Valid 自动校验\n" +
                "    return service.create(req);\n" +
                "}\n\n" +
                "// Request DTO\n" +
                "public class CreateRequest {\n" +
                "    @NotBlank(message = \"名称不能为空\")\n" +
                "    private String name;\n" +
                "    \n" +
                "    @Min(value = 1, message = \"数量必须大于0\")\n" +
                "    private int quantity;\n" +
                "}\n" +
                "```\n",
                "https://docs.spring.io/spring-framework/reference/core/validation/beanvalidation.html"
        ));

        SUGGESTION_REGISTRY.put("内存溢出", new SuggestionTemplate(
                "内存溢出修复",
                "CRITICAL",
                "OOM_FIX",
                "## 内存溢出修复方案\n\n" +
                "### 1. 立即措施\n" +
                "```bash\n" +
                "# 生成 heap dump\n" +
                "jmap -dump:format=b,file=heap.hprof <pid>\n" +
                "```\n\n" +
                "### 2. JVM 参数调优\n" +
                "```bash\n" +
                "java -Xms2g -Xmx4g -XX:+HeapDumpOnOutOfMemoryError \\\n" +
                "     -XX:HeapDumpPath=/logs/heapdump.hprof \\\n" +
                "     -jar app.jar\n" +
                "```\n\n" +
                "### 3. 代码排查\n" +
                "- 检查是否有未关闭的流/连接\n" +
                "- 检查静态集合是否无限增长\n" +
                "- 使用 MAT / JProfiler 分析 heap dump\n",
                "https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/memleaks.html"
        ));

        SUGGESTION_REGISTRY.put("网络连接异常", new SuggestionTemplate(
                "网络连接异常修复",
                "HIGH",
                "NETWORK_FIX",
                "## 网络连接异常修复\n\n" +
                "### 排查步骤\n" +
                "1. `ping` 目标主机确认可达性\n" +
                "2. `telnet host port` 检查端口是否开放\n" +
                "3. 检查防火墙/安全组配置\n" +
                "4. 确认 DNS 解析正常\n\n" +
                "### 代码层面\n" +
                "```java\n" +
                "// 添加重试机制\n" +
                "@Retryable(value = ConnectException.class, maxAttempts = 3)\n" +
                "public Result callRemote() {\n" +
                "    return restTemplate.getForObject(url, Result.class);\n" +
                "}\n" +
                "```\n",
                "https://docs.spring.io/spring-retry/docs/current/reference/html/"
        ));

        SUGGESTION_REGISTRY.put("类加载异常", new SuggestionTemplate(
                "类加载异常修复",
                "HIGH",
                "DEPENDENCY_FIX",
                "## 类加载异常修复\n\n" +
                "### 排查步骤\n" +
                "1. `mvn dependency:tree` 检查依赖树\n" +
                "2. 确认缺失类所在的 jar 包版本\n" +
                "3. 排除冲突传递依赖\n\n" +
                "```xml\n" +
                "<dependency>\n" +
                "    <groupId>...</groupId>\n" +
                "    <artifactId>...</artifactId>\n" +
                "    <exclusions>\n" +
                "        <exclusion>\n" +
                "            <groupId>conflict-group</groupId>\n" +
                "            <artifactId>conflict-artifact</artifactId>\n" +
                "        </exclusion>\n" +
                "    </exclusions>\n" +
                "</dependency>\n" +
                "```\n",
                "https://maven.apache.org/guides/introduction/introduction-to-optional-and-excludes-dependencies.html"
        ));

        SUGGESTION_REGISTRY.put("Spring Bean 缺失", new SuggestionTemplate(
                "Spring Bean 缺失修复",
                "MEDIUM",
                "BEAN_CONFIG",
                "## Spring Bean 缺失修复\n\n" +
                "### 排查\n" +
                "1. 确认类上是否有 @Component/@Service/@Repository/@Controller\n" +
                "2. 确认包扫描路径是否覆盖该类\n" +
                "3. 检查 @ConditionalOnXxx 条件是否满足\n" +
                "4. 检查 @Configuration 类中 @Bean 方法是否被调用\n\n" +
                "```java\n" +
                "@SpringBootApplication\n" +
                "@ComponentScan(basePackages = {\"com.example\"})\n" +
                "public class Application {\n" +
                "}\n" +
                "```\n",
                "https://docs.spring.io/spring-framework/reference/core/beans.html"
        ));
    }

    public RepairSuggestion generate(AnalysisResult analysisResult) {
        log.info("[修复建议Agent] 基于分析结果生成修复建议: analysisId={}", analysisResult.getId());

        RepairSuggestion suggestion = new RepairSuggestion();
        suggestion.setId(UUID.randomUUID().toString());
        suggestion.setAnalysisResultId(analysisResult.getId());
        suggestion.setGeneratedTime(LocalDateTime.now());

        SuggestionTemplate template = SUGGESTION_REGISTRY.get(analysisResult.getRootCauseCategory());

        if (template != null) {
            suggestion.setTitle(template.title);
            suggestion.setSeverity(template.severity);
            suggestion.setFixType(template.fixType);
            suggestion.setReferenceDoc(template.referenceDoc);

            StringBuilder description = new StringBuilder();
            description.append(template.patchTemplate);
            description.append("\n---\n");
            description.append("**原始异常**: ").append(analysisResult.getExceptionType()).append("\n");
            description.append("**异常消息**: ").append(analysisResult.getExceptionMessage()).append("\n");
            description.append("**发生位置**: ").append(analysisResult.getFailingClass())
                    .append(".").append(analysisResult.getFailingMethod())
                    .append(":").append(analysisResult.getFailingLine()).append("\n");
            description.append("**根因分析**: ").append(analysisResult.getRootCauseDescription()).append("\n");
            description.append("**分析置信度**: ").append(String.format("%.1f%%", analysisResult.getConfidenceScore() * 100)).append("\n");
            suggestion.setDescription(description.toString());

            log.info("[修复建议Agent] 匹配到修复模板: {}", template.title);
        } else {
            suggestion.setTitle("未知异常 - 人工排查建议");
            suggestion.setSeverity("MEDIUM");
            suggestion.setFixType("MANUAL_CHECK");
            suggestion.setReferenceDoc("无对应参考文档");

            StringBuilder description = new StringBuilder();
            description.append("## 需要人工排查\n\n");
            description.append("该异常类型未匹配到已知修复模板，建议：\n\n");
            description.append("1. 检查异常堆栈中的关键代码行\n");
            description.append("2. 查看应用日志上下文\n");
            description.append("3. 搜索 ").append(analysisResult.getExceptionType()).append(" 的官方文档\n");
            description.append("4. 检查近期代码变更记录\n\n");
            description.append("**异常类型**: ").append(analysisResult.getExceptionType()).append("\n");
            description.append("**异常消息**: ").append(analysisResult.getExceptionMessage()).append("\n");
            description.append("**发生位置**: ").append(analysisResult.getFailingClass())
                    .append(".").append(analysisResult.getFailingMethod()).append("\n");
            suggestion.setDescription(description.toString());
        }

        return suggestion;
    }

    private static class SuggestionTemplate {
        String title;
        String severity;
        String fixType;
        String patchTemplate;
        String referenceDoc;

        SuggestionTemplate(String title, String severity, String fixType,
                           String patchTemplate, String referenceDoc) {
            this.title = title;
            this.severity = severity;
            this.fixType = fixType;
            this.patchTemplate = patchTemplate;
            this.referenceDoc = referenceDoc;
        }
    }
}
