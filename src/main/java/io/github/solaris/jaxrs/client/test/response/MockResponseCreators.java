package io.github.solaris.jaxrs.client.test.response;

import static jakarta.ws.rs.core.HttpHeaders.LOCATION;
import static jakarta.ws.rs.core.HttpHeaders.RETRY_AFTER;
import static jakarta.ws.rs.core.Response.Status.ACCEPTED;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.GATEWAY_TIMEOUT;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static jakarta.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

public final class MockResponseCreators {
    private MockResponseCreators() {}

    public static MockResponseCreator withSuccess() {
        return new MockResponseCreator(OK);
    }

    public static MockResponseCreator withSuccess(Object entity, MediaType mediaType) {
        MockResponseCreator responseCreator = new MockResponseCreator(OK).entity(entity);
        return mediaType == null ? responseCreator : responseCreator.mediaType(mediaType);
    }

    public static MockResponseCreator withCreated(URI location) {
        return new MockResponseCreator(CREATED).header(LOCATION, location);
    }

    public static MockResponseCreator withAccepted() {
        return new MockResponseCreator(ACCEPTED);
    }

    public static MockResponseCreator withNoContent() {
        return new MockResponseCreator(NO_CONTENT);
    }

    public static MockResponseCreator withBadRequest() {
        return new MockResponseCreator(BAD_REQUEST);
    }

    public static MockResponseCreator withUnauthorized() {
        return new MockResponseCreator(UNAUTHORIZED);
    }

    public static MockResponseCreator withForbidden() {
        return new MockResponseCreator(FORBIDDEN);
    }

    public static MockResponseCreator withNotFound() {
        return new MockResponseCreator(NOT_FOUND);
    }

    public static MockResponseCreator withConflict() {
        return new MockResponseCreator(CONFLICT);
    }

    public static MockResponseCreator withTooManyRequests() {
        return new MockResponseCreator(TOO_MANY_REQUESTS);
    }

    public static MockResponseCreator withTooManyRequests(int retryAfter) {
        return new MockResponseCreator(TOO_MANY_REQUESTS).header(RETRY_AFTER, retryAfter);
    }

    public static MockResponseCreator withInternalServerError() {
        return new MockResponseCreator(INTERNAL_SERVER_ERROR);
    }

    public static MockResponseCreator withServiceUnavailable() {
        return new MockResponseCreator(SERVICE_UNAVAILABLE);
    }

    public static MockResponseCreator withGatewayTimeout() {
        return new MockResponseCreator(GATEWAY_TIMEOUT);
    }

    public static MockResponseCreator withStatus(StatusType status) {
        return new MockResponseCreator(status);
    }

    public static MockResponseCreator withStatus(int statusCode) {
        StatusType statusType = Objects.requireNonNullElseGet(Status.fromStatusCode(statusCode), () -> new CustomStatus(statusCode));
        return new MockResponseCreator(statusType);
    }

    public static ResponseCreator withException(IOException ioe) {
        return request -> {
            throw ioe;
        };
    }

    private record CustomStatus(int statusCode) implements StatusType {

        @Override
        public int getStatusCode() {
            return statusCode;
        }

        @Override
        public Family getFamily() {
            return Family.familyOf(statusCode);
        }

        @Override
        public String getReasonPhrase() {
            return "";
        }
    }
}
