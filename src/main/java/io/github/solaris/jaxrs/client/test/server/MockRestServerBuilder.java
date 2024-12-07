package io.github.solaris.jaxrs.client.test.server;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Configuration;

import io.github.solaris.jaxrs.client.test.manager.OrderedRequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.UnorderedRequestExpectationManager;

public class MockRestServerBuilder {
    private final ClientBuilder clientBuilder;

    private boolean ignoreRequestOrder;

    MockRestServerBuilder(ClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    private RequestExpectationManager getExpectationManager() {
        if (ignoreRequestOrder) {
            return new UnorderedRequestExpectationManager();
        }

        return new OrderedRequestExpectationManager();
    }

    public MockRestServerBuilder ignoreRequestOrder(boolean ignoreRequestOrder) {
        this.ignoreRequestOrder = ignoreRequestOrder;
        return this;
    }

    public MockRestServer build() {
        RequestExpectationManager expectationManager = getExpectationManager();
        Configuration configuration = clientBuilder.getConfiguration();
        if (configuration.isRegistered(MockResponseFilter.class)) {
            configuration.getInstances()
                .stream()
                .filter(MockResponseFilter.class::isInstance)
                .map(MockResponseFilter.class::cast)
                .findFirst()
                .orElseThrow()
                .setExpectationManager(expectationManager);
        } else {
            clientBuilder.register(new MockResponseFilter(expectationManager), Integer.MAX_VALUE);
        }

        return new MockRestServer(expectationManager);
    }
}
