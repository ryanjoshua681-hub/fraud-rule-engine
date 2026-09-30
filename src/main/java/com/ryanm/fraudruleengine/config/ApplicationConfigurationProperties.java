package com.ryanm.fraudruleengine.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("app")
public class ApplicationConfigurationProperties {

    private Kafka kafka = new Kafka();
    private Cache cache = new Cache();
    private Rules rules = new Rules();

    @Data
    public static class Kafka {

        private Outgoing outgoing = new Outgoing();

        @Data
        public static class Outgoing {

            private Topics topics = new Topics();

            @Data
            public static class Topics {

                private String rawTransactions = "local-transactions.raw";
                private String fraudFlags = "local-fraud.flags";
                private String dlq = "local-transactions.dlq";
            }
        }
    }

    @Data
    public static class Cache {

        private int hours = 12;
        private int initialCapacity = 50;
        private int maximumSize = 300;
    }

    @Data
    public static class Rules {

        /** Transactions above this amount (in ZAR) contribute to the high-value rule score. */
        private BigDecimal highValueThreshold = BigDecimal.valueOf(50_000);

        /** Time window in seconds for velocity checks. */
        private int velocityWindowSeconds = 60;

        /** Maximum number of transactions allowed within the velocity window. */
        private int velocityMaxTransactions = 5;

        /** Transactions outside these hours (24h, Africa/Johannesburg) trigger the unusual-hours rule. */
        private int unusualHoursStart = 23;

        private int unusualHoursEnd = 5;

        /** Amounts that are exact round numbers above this threshold trigger the round-amount rule. */
        private BigDecimal roundAmountThreshold = BigDecimal.valueOf(10_000);

        /** Merchant codes that are permanently blocked. */
        private List<String> blacklistedMerchants = new ArrayList<>();
    }
}
