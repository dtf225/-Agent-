# -Agent-
我构建了一个基于 Spring Boot 与多 Agent 协作的智能运维系统。核心痛点在于传统后端服务日志分散，故障排查耗时且依赖人工经验。
### 配置钉钉/飞书通知
在 application.yml 中填入 webhook URL 并将 enabled 设为 true ：

```
agent:
  notification:
    enabled: true
    dingtalk:
      webhook: "https://oapi.
      dingtalk.com/robot/send?
      access_token=xxx"
    feishu:
      webhook: "https://open.feishu.
      cn/open-apis/bot/v2/hook/xxx
```
