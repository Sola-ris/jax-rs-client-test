package io.github.solaris.jaxrs.client.test.server;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Providers;

import org.jspecify.annotations.Nullable;

import io.github.solaris.jaxrs.client.test.manager.RequestExpectationManager;
import io.github.solaris.jaxrs.client.test.request.ClientEntityConverter;
import io.github.solaris.jaxrs.client.test.request.EntityConverter;
import io.github.solaris.jaxrs.client.test.request.ProvidersEntityConverter;

/**
 * <p>Filter that redirects the current request to a {@link RequestExpectationManager} bound via {@link MockRestServer}.</p>
 * <p>Not intend to for public use, but must be declared {@code public} so JAX-RS implementations can instantiate it.</p>
 */
public final class MockResponseFilter implements ClientRequestFilter {

    @Context
    private @Nullable Providers providers;

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
