package io.github.solaris.jaxrs.client.test.server;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Providers;

import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.request.ClientEntityConverter;
import io.github.solaris.jaxrs.client.test.request.EntityConverter;
import io.github.solaris.jaxrs.client.test.request.ProvidersEntityConverter;

public class MockResponseFilter implements ClientRequestFilter {

    @Context
    private Providers providers;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (providers != null) {
            requestContext.setProperty(EntityConverter.class.getName(), new ProvidersEntityConverter(providers));
        } else {
            requestContext.setProperty(EntityConverter.class.getName(), new ClientEntityConverter());
        }

        Object property = requestContext.getConfiguration().getProperty(RequestExpectationManager.class.getName());
        if (property instanceof RequestExpectationManager expectationManager) {
            requestContext.abortWith(
                expectationManager.validateRequest(requestContext)
            );
        }
    }
}
