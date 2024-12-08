package io.github.solaris.jaxrs.client.test.server;

import java.time.Duration;

import jakarta.ws.rs.core.Configurable;

import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.request.ExpectedCount;
import io.github.solaris.jaxrs.client.test.request.RequestMatcher;
import io.github.solaris.jaxrs.client.test.response.ResponseActions;

public final class MockRestServer {
    private final RequestExpectationManager expectationManager;

    MockRestServer(RequestExpectationManager expectationManager) {
        this.expectationManager = expectationManager;
    }

    public static MockRestServerBuilder bindTo(Configurable<?> configurable) {
        return new MockRestServerBuilder(configurable);
    }

    public ResponseActions expect(RequestMatcher requestMatcher) {
        return expect(ExpectedCount.once(), requestMatcher);
    }

    public ResponseActions expect(ExpectedCount count, RequestMatcher matcher) {
        return expectationManager.expectRequest(count, matcher);
    }

    public void verify() {
        expectationManager.verify();
    }

    public void verify(Duration timeout) {
        expectationManager.verify(timeout);
    }

    public void reset() {
        expectationManager.reset();
    }
}
