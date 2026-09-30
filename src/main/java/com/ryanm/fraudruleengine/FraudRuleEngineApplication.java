package com.ryanm.fraudruleengine;

import com.ryanm.fraudruleengine.config.ApplicationConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

@SpringBootApplication
@EnableJdbcAuditing
@EnableJdbcRepositories
@EnableConfigurationProperties(ApplicationConfigurationProperties.class)
@SuppressWarnings("HideUtilityClassConstructor")
public class FraudRuleEngineApplication {

    public static void main(final String[] args) {
        SpringApplication.run(FraudRuleEngineApplication.class, args);
    }
}
