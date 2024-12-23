package io.github.solaris.jaxrs.client.test.response;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.internal.ClientCleaner;

public class ExecutingResponseCreator implements ResponseCreator {
    private final Client client;

    public ExecutingResponseCreator() {
        this.client = ClientBuilder.newClient();
        ClientCleaner.register(this, client);
    }

    public ExecutingResponseCreator(Client client) {
        this.client = client;
    }

    @Override
    public Response createResponse(ClientRequestContext request) {
        Invocation.Builder invocationBuilder = client.target(request.getUri())
            .request()
            .headers(request.getHeaders());
        if (request.hasEntity()) {
            return invocationBuilder.method(request.getMethod(), Entity.entity(request.getEntity(), request.getMediaType()));
        }
        return invocationBuilder.method(request.getMethod());
    }
}
