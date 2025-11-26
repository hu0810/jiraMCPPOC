package com.example.mcp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class JiraMcpTools {

        private static final Logger log = LoggerFactory.getLogger(JiraMcpTools.class);

        private final WebClient webClient;
        private final String projectKey;
        private final String baseUrl;

        public JiraMcpTools(
                        @Value("${incident.jira.base-url}") String baseUrl,
                        @Value("${incident.jira.user-email}") String userEmail,
                        @Value("${incident.jira.api-token}") String apiToken,
                        @Value("${incident.jira.project-key}") String projectKey) {
                this.projectKey = projectKey;
                this.baseUrl = baseUrl;

                String basicAuth = java.util.Base64.getEncoder()
                                .encodeToString((userEmail + ":" + apiToken).getBytes());

                this.webClient = WebClient.builder()
                                .baseUrl(baseUrl)
                                .defaultHeader("Authorization", "Basic " + basicAuth)
                                .defaultHeader("Accept", "application/json")
                                .build();

                log.info("✅ JiraMcpTools initialized. baseUrl={}, projectKey={}", baseUrl, projectKey);
        }

        public record JiraIssueResult(String key, String url) {
        }

        @Tool(description = "建立一張 Jira issue。通常用來記錄 incident 或錯誤。")
        public JiraIssueResult createJiraTicket(
                        @ToolParam(description = "Issue 標題", required = true) String summary,
                        @ToolParam(description = "Issue 詳細描述，建議包含錯誤訊息、service 名稱、namespace 等", required = true) String description,
                        @ToolParam(description = "嚴重度，例如: LOW/MEDIUM/HIGH，可選", required = false) String severity) {
                log.info("🧰 [createJiraTicket] summary='{}', severity='{}'", summary, severity);

                String finalSummary = "[k8s incident] " + summary;
                String finalDesc = description + "\n\nSeverity: " + (severity == null ? "MEDIUM" : severity);

                Map<String, Object> descriptionAdf = Map.of(
                                "type", "doc",
                                "version", 1,
                                "content", List.of(
                                                Map.of(
                                                                "type", "paragraph",
                                                                "content", List.of(
                                                                                Map.of(
                                                                                                "type", "text",
                                                                                                "text", finalDesc)))));

                Map<String, Object> fields = Map.of(
                                "project", Map.of("key", projectKey),
                                "summary", finalSummary,
                                "description", descriptionAdf,
                                "issuetype", Map.of("name", "Task"));

                Map<String, Object> payload = Map.of("fields", fields);

                try {
                        Map<?, ?> resp = webClient.post()
                                        .uri("/rest/api/3/issue")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(payload)
                                        .retrieve()
                                        .bodyToMono(Map.class)
                                        .block();

                        log.info("✅ Jira issue created. response={}", resp);

                        String key = String.valueOf(resp.get("key"));
                        String url = baseUrl + "/browse/" + key;
                        return new JiraIssueResult(key, url);

                } catch (WebClientResponseException e) {
                        log.error("❌ Jira API error. status={}, body={}",
                                        e.getStatusCode(), e.getResponseBodyAsString(), e);
                        throw e;
                } catch (Exception e) {
                        log.error("❌ Unexpected error when calling Jira API", e);
                        throw e;
                }
        }

}
