package io.github.solaris.jaxrs.client.test.request;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

public final class EntityRequestMatchers {

    EntityRequestMatchers() {}

    public RequestMatcher mediaType(String mediaType) {
        return mediaType(MediaType.valueOf(mediaType));
    }

    public RequestMatcher mediaType(MediaType mediaType) {
        return request -> {
            MediaType actual = request.getMediaType();
            assertTrue("MediaType was not set.", actual != null);
            assertEqual("MediaType", mediaType, actual);
        };
    }

    public RequestMatcher isEqualTo(Object expected) {
        return request -> assertEqual("Entity", expected, request.getEntity());
    }

    public RequestMatcher string(String expected) {
        return request -> {
            EntityConverter entityConverter = EntityConverter.fromRequestContext(request);
            String actual = entityConverter.convertEntity(request, String.class);
            assertEqual("Entity String", expected, actual);
        };
    }

    public RequestMatcher form(Form expectedForm) {
        return request -> {
            EntityConverter entityConverter = EntityConverter.fromRequestContext(request);
            Form form = entityConverter.convertEntity(request, Form.class);
            assertEqual("Form", expectedForm.asMap(), form.asMap());
        };
    }

    public RequestMatcher formContains(Form expectedForm) {
        return request -> {
            EntityConverter entityConverter = EntityConverter.fromRequestContext(request);
            MultivaluedMap<String, String> expectedMap = expectedForm.asMap();
            MultivaluedMap<String, String> actualMap = entityConverter.convertEntity(request, Form.class).asMap();

            assertTrue("Expected " + expectedMap + " to be smaller or the same size as " + actualMap, expectedMap.size() <= actualMap.size());
            for (Map.Entry<String, List<String>> entry : expectedMap.entrySet()) {
                String name = entry.getKey();
                List<String> values = entry.getValue();

                assertTrue("Expected " + actualMap + " to contain parameter '" + name + "'", actualMap.get(name) != null);
                assertTrue("Expected " + values + " to be smaller or the same size as " + actualMap.get(name),
                    values.size() <= actualMap.get(name).size());
                assertTrue("Expected " + values + " to be a subset of " + actualMap.get(name), actualMap.get(name).containsAll(values));
            }
        };
    }

    private static void assertEqual(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}