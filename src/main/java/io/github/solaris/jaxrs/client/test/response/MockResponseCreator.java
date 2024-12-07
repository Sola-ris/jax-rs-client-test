package io.github.solaris.jaxrs.client.test.response;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.StatusType;


public class MockResponseCreator implements ResponseCreator {
    private final StatusType status;

    private Object entity;
    private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    private final List<NewCookie> cookies = new ArrayList<>();

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

    @Override
    public Response createResponse(ClientRequestContext request) {
        return Response.status(status)
            .entity(entity)
            .replaceAll(headers)
            .cookie(cookies.toArray(new NewCookie[0]))
            .build();
    }
}
