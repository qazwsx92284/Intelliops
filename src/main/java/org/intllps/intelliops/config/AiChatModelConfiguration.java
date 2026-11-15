package org.intllps.intelliops.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatModelConfiguration {

    @Bean(name = "chatModel")
    @ConditionalOnMissingBean(name = "chatModel")
    public ChatModel fallbackChatModel(AiProperties aiProperties) {
        return new FallbackChatModel(aiProperties);
    }

    private static final class FallbackChatModel implements ChatModel {
        private final AiProperties aiProperties;

        private FallbackChatModel(AiProperties aiProperties) {
            this.aiProperties = aiProperties;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
                throw new IllegalStateException("AI API key is not configured. Set INTELLIOPS_AI_API_KEY or OPENAI_API_KEY.");
            }
            throw new IllegalStateException("AI model is not available in this environment. Configure a valid Spring AI OpenAI model provider.");
        }
    }
}
