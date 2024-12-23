package io.github.solaris.jaxrs.client.test.request;

import java.net.URI;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.internal.ClientCleaner;

public final class ClientEntityConverter extends EntityConverter {
    private static final URI LOCALHOST = URI.create("http://localhost");
    private static final RoundTripFilter ROUND_TRIP_FILTER = new RoundTripFilter();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) {
        if (canShortCircuit(requestContext, type, null)) {
            return (T) requestContext.getEntity();
        }

        try (Response response = convertEntity(requestContext)) {
            return response.readEntity(type);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) {
        if (canShortCircuit(requestContext, genericType.getRawType(), genericType.getType())) {
            return (T) requestContext.getEntity();
        }

        try (Response response = convertEntity(requestContext)) {
            return response.readEntity(genericType);
        }
    }

    private Response convertEntity(ClientRequestContext requestContext) {
        // Directly closing the 'inner' client here causes Jersey to close the 'outer' client as well
        Client client = ClientBuilder.newClient().register(ROUND_TRIP_FILTER);
        ClientCleaner.register(this, client);
        return client.target(LOCALHOST)
            .request(requestContext.getMediaType())
            .post(Entity.entity(requestContext.getEntity(), requestContext.getMediaType()));
    }

    private static final class RoundTripFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext requestContext) {
            requestContext.abortWith(Response.ok(requestContext.getEntity(), requestContext.getMediaType()).build());
        }
    }
}
