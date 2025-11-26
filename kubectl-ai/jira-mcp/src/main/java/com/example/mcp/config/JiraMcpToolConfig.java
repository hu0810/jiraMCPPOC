// src/main/java/com/example/mcp/config/JiraMcpToolConfig.java
package com.example.mcp.config;

import com.example.mcp.service.JiraMcpTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JiraMcpToolConfig {

    @Bean
    public ToolCallbackProvider jiraTools(JiraMcpTools jiraMcpTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(jiraMcpTools)
                .build();
    }
}
