package io.github.solaris.jaxrs.client.test.server;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;

public class MockResponseFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        Object property = requestContext.getConfiguration().getProperty(RequestExpectationManager.class.getName());
        if (property instanceof RequestExpectationManager expectationManager) {
            requestContext.abortWith(
                expectationManager.validateRequest(requestContext)
            );
        }
    }
}
