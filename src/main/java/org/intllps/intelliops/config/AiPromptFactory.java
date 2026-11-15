package org.intllps.intelliops.config;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AiPromptFactory {

    @Bean
    public PromptTemplate incidentAnalysisPromptTemplate() {
        try {
            String template = new ClassPathResource("prompts/incident-analysis-prompt.txt").getContentAsString(StandardCharsets.UTF_8);
            return new PromptTemplate(template);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load AI prompt template", ex);
        }
    }
}

