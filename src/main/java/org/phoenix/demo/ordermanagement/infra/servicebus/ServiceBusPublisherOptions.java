package org.phoenix.demo.ordermanagement.infra.servicebus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.servicebus.publisher")
public class ServiceBusPublisherOptions {

    private String topicName = "order-events";
    private boolean enabled = true;

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}