package io.github.solaris.jaxrs.client.test.response;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.core.Variant;

public class MockResponseCreator implements ResponseCreator {
    private final StatusType status;

    private Object entity;
    private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    private final List<NewCookie> cookies = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final List<Variant> variants = new ArrayList<>();

    MockResponseCreator(StatusType status) {
        this.status = status;
    }

    public MockResponseCreator entity(Object entity) {
        this.entity = entity;
        return this;
    }

    public MockResponseCreator mediaType(MediaType mediaType) {
        headers.putSingle(CONTENT_TYPE, mediaType.toString());
        return this;
    }

    public MockResponseCreator header(String name, Object... values) {
        for (Object value : values) {
            headers.add(name, value);
        }
        return this;
    }

    public MockResponseCreator cookies(NewCookie... cookies) {
        this.cookies.addAll(Arrays.asList(cookies));
        return this;
    }

    public MockResponseCreator links(Link... links) {
        this.links.addAll(Arrays.asList(links));
        return this;
    }

    public MockResponseCreator variants(Variant... variants) {
        this.variants.addAll(Arrays.asList(variants));
        return this;
    }

    @Override
    public Response createResponse(ClientRequestContext request) {
        Response.ResponseBuilder responseBuilder = Response.status(status)
            .entity(entity)
            .replaceAll(headers)
            .links(links.toArray(new Link[0]))
            .cookie(cookies.toArray(new NewCookie[0]));

        // RestEasy Reactive throws an NPE when passed an empty array of Variants
        if (!variants.isEmpty()) {
            responseBuilder.variants(variants.toArray(new Variant[0]));
        }
        return responseBuilder.build();
    }
}
