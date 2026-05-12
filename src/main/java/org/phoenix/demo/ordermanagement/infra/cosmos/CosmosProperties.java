package org.phoenix.demo.ordermanagement.infra.cosmos;

public class CosmosProperties {

    private String uri;
    private String key;
    private String database;
    private String ordersContainer;
    private String leasesContainer = "leases";

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getOrdersContainer() {
        return ordersContainer;
    }

    public void setOrdersContainer(String ordersContainer) {
        this.ordersContainer = ordersContainer;
    }

    public String getLeasesContainer() {
        return leasesContainer;
    }

    public void setLeasesContainer(String leasesContainer) {
        this.leasesContainer = leasesContainer;
    }
}
