package io.github.solaris.jaxrs.client.test.request;

import jakarta.ws.rs.client.ClientRequestContext;


@FunctionalInterface
public interface RequestMatcher {

    void match(ClientRequestContext request);
}
