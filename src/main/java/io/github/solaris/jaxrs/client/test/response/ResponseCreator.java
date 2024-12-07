package io.github.solaris.jaxrs.client.test.response;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Response;

@FunctionalInterface
public interface ResponseCreator {

    Response createResponse(ClientRequestContext request) throws IOException;
}
