package org.phoenix.demo.ordermanagement.infra.changefeed;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.outbox.changefeed")
public class OutboxChangeFeedOptions {

    private String processorName = "outbox-dispatcher";
    private String instanceName = defaultInstanceName();
    private Duration feedPollDelay = Duration.ofSeconds(1);
    private int maxItemsPerBatch = 100;

    private static String defaultInstanceName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "local-instance";
        }
    }

    public String getProcessorName() {
        return processorName;
    }

    public void setProcessorName(String processorName) {
        this.processorName = processorName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public Duration getFeedPollDelay() {
        return feedPollDelay;
    }

    public void setFeedPollDelay(Duration feedPollDelay) {
        this.feedPollDelay = feedPollDelay;
    }

    public int getMaxItemsPerBatch() {
        return maxItemsPerBatch;
    }

    public void setMaxItemsPerBatch(int maxItemsPerBatch) {
        this.maxItemsPerBatch = maxItemsPerBatch;
    }
}
