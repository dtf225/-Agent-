package com.example.agent.agent.repair;

import com.example.agent.model.RepairSuggestion;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    @Value("${agent.notification.dingtalk.webhook:}")
    private String dingTalkWebhook;

    @Value("${agent.notification.feishu.webhook:}")
    private String feiShuWebhook;

    @Value("${agent.notification.enabled:true}")
    private boolean notificationEnabled;

    public NotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    public void sendNotification(RepairSuggestion suggestion) {
        if (!notificationEnabled) {
            log.info("[通知服务] 通知功能已禁用，跳过推送: suggestionId={}", suggestion.getId());
            return;
        }

        boolean sentToDingTalk = sendToDingTalk(suggestion);
        boolean sentToFeiShu = sendToFeiShu(suggestion);

        if (sentToDingTalk || sentToFeiShu) {
            suggestion.setNotificationSent(true);
            log.info("[通知服务] 修复建议已推送: suggestionId={}, dingtalk={}, feishu={}",
                    suggestion.getId(), sentToDingTalk, sentToFeiShu);
        } else {
            log.warn("[通知服务] 所有推送渠道均不可用，请配置 webhook URL");
        }
    }

    private boolean sendToDingTalk(RepairSuggestion suggestion) {
        if (dingTalkWebhook == null || dingTalkWebhook.isEmpty()) {
            log.debug("[通知服务] 未配置钉钉 Webhook，跳过");
            return false;
        }

        try {
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", "【" + suggestion.getSeverity() + "】" + suggestion.getTitle());
            markdown.put("text", buildDingTalkMarkdown(suggestion));

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "markdown");
            payload.put("markdown", markdown);

            String json = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(dingTalkWebhook)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("[通知服务] 钉钉推送成功");
                    return true;
                } else {
                    log.warn("[通知服务] 钉钉推送失败: HTTP {}", response.code());
                }
            }
        } catch (IOException e) {
            log.error("[通知服务] 钉钉推送异常", e);
        }
        return false;
    }

    private boolean sendToFeiShu(RepairSuggestion suggestion) {
        if (feiShuWebhook == null || feiShuWebhook.isEmpty()) {
            log.debug("[通知服务] 未配置飞书 Webhook，跳过");
            return false;
        }

        try {
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> post = new HashMap<>();
            Map<String, String> title = new HashMap<>();
            title.put("tag", "text");
            title.put("text", "【" + suggestion.getSeverity() + "】" + suggestion.getTitle());
            post.put("title", suggestion.getTitle());

            java.util.List<Map<String, Object>> contentList = new java.util.ArrayList<>();

            Map<String, Object> textLine = new HashMap<>();
            textLine.put("tag", "text");
            textLine.put("text", buildFeiShuText(suggestion));
            contentList.add(textLine);

            post.put("content", contentList);

            Map<String, Object> postContent = new HashMap<>();
            postContent.put("zh_cn", post);
            content.put("post", postContent);

            Map<String, Object> payload = new HashMap<>();
            payload.put("msg_type", "post");
            payload.put("content", content);

            String json = objectMapper.writeValueAsString(payload);
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(feiShuWebhook)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("[通知服务] 飞书推送成功");
                    return true;
                } else {
                    log.warn("[通知服务] 飞书推送失败: HTTP {}", response.code());
                }
            }
        } catch (IOException e) {
            log.error("[通知服务] 飞书推送异常", e);
        }
        return false;
    }

    private String buildDingTalkMarkdown(RepairSuggestion suggestion) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🚨 智能运维告警\n\n");
        sb.append("**告警级别**: ").append(suggestion.getSeverity()).append("\n\n");
        if (suggestion.getDescription() != null) {
            int maxLen = Math.min(suggestion.getDescription().length(), 1500);
            sb.append(suggestion.getDescription(), 0, maxLen);
            if (suggestion.getDescription().length() > 1500) {
                sb.append("\n\n> 内容过长已截断，请查看完整报告");
            }
        }
        if (suggestion.getReferenceDoc() != null && !suggestion.getReferenceDoc().isEmpty()) {
            sb.append("\n\n📖 **参考文档**: ").append(suggestion.getReferenceDoc());
        }
        sb.append("\n\n---\n⏰ 生成时间: ").append(suggestion.getGeneratedTime());
        return sb.toString();
    }

    private String buildFeiShuText(RepairSuggestion suggestion) {
        StringBuilder sb = new StringBuilder();
        sb.append("告警级别: ").append(suggestion.getSeverity()).append("\n");
        if (suggestion.getDescription() != null) {
            int maxLen = Math.min(suggestion.getDescription().length(), 1500);
            sb.append(suggestion.getDescription(), 0, maxLen);
            if (suggestion.getDescription().length() > 1500) {
                sb.append("\n...内容过长已截断");
            }
        }
        return sb.toString();
    }
}
