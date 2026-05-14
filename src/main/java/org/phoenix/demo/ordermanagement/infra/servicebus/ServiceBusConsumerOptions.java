package org.phoenix.demo.ordermanagement.infra.servicebus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.servicebus.consumer")
public class ServiceBusConsumerOptions {

    private boolean enabled = false;
    private String topicName = "order-events";
    private String subscriptionName = "demo-consumer";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }
}