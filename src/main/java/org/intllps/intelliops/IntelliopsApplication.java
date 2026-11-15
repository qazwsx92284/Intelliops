package org.intllps.intelliops;

import org.intllps.intelliops.config.AiProperties;
import org.intllps.intelliops.config.IncidentDetectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({IncidentDetectionProperties.class, AiProperties.class})
public class IntelliopsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntelliopsApplication.class, args);
    }

}