package org.phoenix.demo.ordermanagement.infra.cosmos;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.spring.data.cosmos.CosmosFactory;
import com.azure.spring.data.cosmos.config.AbstractCosmosConfiguration;
import com.azure.spring.data.cosmos.repository.config.EnableCosmosRepositories;
import org.phoenix.demo.ordermanagement.application.abstractions.OutboxPayloadSerializer;
import org.phoenix.demo.ordermanagement.application.outbox.OutboxMapper;
import org.phoenix.demo.ordermanagement.infra.changefeed.OutboxChangeFeedOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCosmosRepositories(basePackages = "org.phoenix.demo.ordermanagement.infra.cosmos")
@EnableConfigurationProperties(OutboxChangeFeedOptions.class)
public class OrderManagementCosmosConfiguration extends AbstractCosmosConfiguration {

    private final CosmosProperties properties;

    public OrderManagementCosmosConfiguration(
            @Qualifier("orderManagementCosmosProperties") CosmosProperties properties) {
        this.properties = properties;
    }

    @ConfigurationProperties(prefix = "app.cosmos")
    @Bean("orderManagementCosmosProperties")
    public static CosmosProperties orderManagementCosmosProperties() {
        return new CosmosProperties();
    }

    @Override
    protected String getDatabaseName() {
        return properties.getDatabase();
    }

    @Bean
    public CosmosClientBuilder orderManagementCosmosClientBuilder() {
        return new CosmosClientBuilder()
                .key(properties.getKey())
                .endpoint(properties.getUri());
    }

    @Bean
    public CosmosAsyncClient orderManagementCosmosAsyncClient(
            CosmosClientBuilder orderManagementCosmosClientBuilder) {
        return CosmosFactory.createCosmosAsyncClient(orderManagementCosmosClientBuilder);
    }

    @Bean
    public OutboxMapper outboxMapper(OutboxPayloadSerializer serializer) {
        return new OutboxMapper(serializer);
    }

    @Bean
    public CommandLineRunner ensureLeasesContainer(
            @Qualifier("orderManagementCosmosAsyncClient") CosmosAsyncClient client) {
        return args -> client
                .getDatabase(properties.getDatabase())
                .createContainerIfNotExists(new CosmosContainerProperties(properties.getLeasesContainer(), "/id"))
                .block();
    }
}