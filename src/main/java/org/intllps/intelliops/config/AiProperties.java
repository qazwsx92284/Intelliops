package org.intllps.intelliops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@ConfigurationProperties(prefix = "intelliops.ai")
public class AiProperties {

    private String model = "gpt-4o-mini";
    private int maxTokens = 800;
    private double temperature = 0.2;
    private String apiKey;
    private String baseUrl;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }

        String resolved = firstNonBlank(
                System.getenv("INTELLIOPS_AI_API_KEY"),
                System.getenv("OPENAI_API_KEY"),
                System.getenv("OPENAI_SECRET_KEY"),
                System.getenv("SPRING_AI_OPENAI_API_KEY")
        );
        if (resolved != null && !resolved.isBlank()) {
            this.apiKey = resolved;
        }
        return this.apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }

        String resolved = firstNonBlank(
                System.getenv("INTELLIOPS_AI_BASE_URL"),
                System.getenv("OPENAI_BASE_URL"),
                "https://api.openai.com"
        );
        if (resolved != null && !resolved.isBlank()) {
            this.baseUrl = resolved;
        }
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private static String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }
}
