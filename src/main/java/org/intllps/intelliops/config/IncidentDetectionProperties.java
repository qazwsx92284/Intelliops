package org.intllps.intelliops.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "intelliops.incident-detection")
public class IncidentDetectionProperties {

    private int minErrorCount = 3;
    private int timeWindowMinutes = 10;
    private double errorRateThreshold = 0.5;
    private int repeatedExceptionThreshold = 2;
    private int traceIdThreshold = 2;

    public int getMinErrorCount() {
        return minErrorCount;
    }

    public void setMinErrorCount(int minErrorCount) {
        this.minErrorCount = minErrorCount;
    }

    public int getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    public void setTimeWindowMinutes(int timeWindowMinutes) {
        this.timeWindowMinutes = timeWindowMinutes;
    }

    public double getErrorRateThreshold() {
        return errorRateThreshold;
    }

    public void setErrorRateThreshold(double errorRateThreshold) {
        this.errorRateThreshold = errorRateThreshold;
    }

    public int getRepeatedExceptionThreshold() {
        return repeatedExceptionThreshold;
    }

    public void setRepeatedExceptionThreshold(int repeatedExceptionThreshold) {
        this.repeatedExceptionThreshold = repeatedExceptionThreshold;
    }

    public int getTraceIdThreshold() {
        return traceIdThreshold;
    }

    public void setTraceIdThreshold(int traceIdThreshold) {
        this.traceIdThreshold = traceIdThreshold;
    }
}

