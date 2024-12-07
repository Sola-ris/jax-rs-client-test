package io.github.solaris.jaxrs.client.test.server;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;

class MockResponseFilter implements ClientRequestFilter {
    private RequestExpectationManager expectationManager;

    public MockResponseFilter(RequestExpectationManager expectationManager) {
        this.expectationManager = expectationManager;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.abortWith(
            expectationManager.validateRequest(requestContext)
        );
    }

    public void setExpectationManager(RequestExpectationManager expectationManager) {
        this.expectationManager = expectationManager;
    }
}
