package org.phoenix.demo.ordermanagement.infra.servicebus;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ServiceBusPublisherOptions.class, ServiceBusConsumerOptions.class})
public class ServiceBusConfiguration {
}