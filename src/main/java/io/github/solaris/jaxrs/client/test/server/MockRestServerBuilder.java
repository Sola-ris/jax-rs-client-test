package io.github.solaris.jaxrs.client.test.server;

import jakarta.ws.rs.core.Configurable;

import io.github.solaris.jaxrs.client.test.manager.OrderedRequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.UnorderedRequestExpectationManager;

public class MockRestServerBuilder {
    private final Configurable<?> configurable;

    private boolean ignoreRequestOrder;

    MockRestServerBuilder(Configurable<?> configurable) {
        this.configurable = configurable;
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
        if (!configurable.getConfiguration().isRegistered(MockResponseFilter.class)) {
            configurable.register(MockResponseFilter.class, Integer.MAX_VALUE);
        }
        configurable.property(RequestExpectationManager.class.getName(), expectationManager);

        return new MockRestServer(expectationManager);
    }
}
