package io.github.solaris.jaxrs.client.test.server;

import jakarta.ws.rs.core.Configurable;

import io.github.solaris.jaxrs.client.test.manager.OrderedRequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.StrictlyOrderedRequestExpectationManager;
import io.github.solaris.jaxrs.client.test.manager.UnorderedRequestExpectationManager;

public class MockRestServerBuilder {
    private final Configurable<?> configurable;

    private RequestOrder order = RequestOrder.ORDERED;

    MockRestServerBuilder(Configurable<?> configurable) {
        this.configurable = configurable;
    }

    public MockRestServerBuilder withRequestOrder(RequestOrder order) {
        this.order = order;
        return this;
    }

    public MockRestServer build() {
        RequestExpectationManager expectationManager = switch (order) {
            case ORDERED -> new OrderedRequestExpectationManager();
            case UNORDERED -> new UnorderedRequestExpectationManager();
            case STRICT -> new StrictlyOrderedRequestExpectationManager();
        };

        if (!configurable.getConfiguration().isRegistered(MockResponseFilter.class)) {
            configurable.register(MockResponseFilter.class, Integer.MAX_VALUE);
        }
        configurable.property(RequestExpectationManager.class.getName(), expectationManager);

        return new MockRestServer(expectationManager);
    }
}
