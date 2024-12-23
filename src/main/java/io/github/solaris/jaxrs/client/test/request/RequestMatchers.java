package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertEqual;
import static io.github.solaris.jaxrs.client.test.internal.Assertions.fail;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

public final class RequestMatchers {
    private RequestMatchers() {}

    public static RequestMatcher anything() {
        return request -> {};
    }

    public static RequestMatcher method(String httpMethod) {
        return request -> assertEqual("Unexpected Method.", httpMethod, request.getMethod());
    }

    public static RequestMatcher requestTo(String uri) {
        return request -> assertEqual("Unexpected Request.", URI.create(uri), request.getUri());
    }

    public static RequestMatcher requestTo(URI uri) {
        return request -> assertEqual("Unexpected Request.", uri, request.getUri());
    }

    public static RequestMatcher queryParam(String name, String... expectedValues) {
        return request -> {
            MultivaluedMap<String, String> queryParams = getQueryParams(request.getUri());
            assertCount("QueryParam", name, queryParams, expectedValues.length);

            for (int i = 0; i < expectedValues.length; i++) {
                assertEqual("QueryParam [" + name + "]", expectedValues[i], queryParams.get(name).get(i));
            }
        };
    }

    public static RequestMatcher header(String name, String... expectedValues) {
        return request -> {
            assertCount("header", name, request.getStringHeaders(), expectedValues.length);

            List<String> actualValues = request.getStringHeaders().get(name);
            for (int i = 0; i < expectedValues.length; i++) {
                assertEqual("Request header [" + name + "]", expectedValues[i], actualValues.get(i));
            }
        };
    }

    public static RequestMatcher headerDoesNotExist(String name) {
        return request -> {
            List<Object> headerValues = request.getHeaders().get(name);
            if (headerValues != null) {
                fail("Expected header <" + name + "> to not exist, but it exists with values: " + headerValues);
            }
        };
    }

    public static EntityRequestMatchers entity() {
        return new EntityRequestMatchers();
    }

    private static MultivaluedMap<String, String> getQueryParams(URI uri) {
        return Arrays.stream(uri.getQuery().split("&"))
            .map(query -> URLDecoder.decode(query, UTF_8))
            .map(query -> query.split("="))
            .collect(MultivaluedHashMap::new, (map, query) -> map.add(query[0], query[1]), MultivaluedMap::putAll);
    }

    private static void assertCount(String valueType, String name, MultivaluedMap<String, String> map, int count) {
        List<String> values = map.get(name);
        String message = "Expected " + valueType + " <" + name + ">";
        if (values == null) {
            fail(message + " to exist but was null");
        }
        if (count > values.size()) {
            fail(message + " to have at least <" + count + "> values but found " + values);
        }
    }
}
