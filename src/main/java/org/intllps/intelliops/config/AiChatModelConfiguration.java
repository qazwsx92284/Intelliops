package org.intllps.intelliops.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatModelConfiguration {

    @Bean(name = "chatModel")
    @ConditionalOnMissingBean(name = "chatModel")
    public ChatModel openAiChatModel(AiProperties aiProperties) {
        String apiKey = aiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return new FallbackChatModel(aiProperties);
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(aiProperties.getBaseUrl())
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(aiProperties.getModel())
                .temperature(aiProperties.getTemperature())
                .maxTokens(aiProperties.getMaxTokens())
                .build();

        return new OpenAiChatModel(openAiApi, options);
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
