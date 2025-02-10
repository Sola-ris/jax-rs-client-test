package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.internal.Assertions.assertEqual;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Static factory methods for the built-in {@link RequestMatcher} implementations.
 */
public final class RequestMatchers {
    private RequestMatchers() {}

    /**
     * Match any request.
     */
    public static RequestMatcher anything() {
        return request -> {};
    }

    /**
     * Match the request method.
     *
     * @param httpMethod The HTTP method / verb
     * @see jakarta.ws.rs.HttpMethod HttpMethod
     */
    public static RequestMatcher method(String httpMethod) {
        return request -> assertEqual("Unexpected Method.", httpMethod, request.getMethod());
    }

    /**
     * Match the request URI to the given string.
     *
     * @param uri The expected URI string
     */
    public static RequestMatcher requestTo(String uri) {
        return request -> assertEqual("Unexpected Request.", URI.create(uri), request.getUri());
    }

    /**
     * Match the request URI to the given URI.
     *
     * @param uri The expected URI
     */
    public static RequestMatcher requestTo(URI uri) {
        return request -> assertEqual("Unexpected Request.", uri, request.getUri());
    }

    /**
     * <p>Assert the values of a single query parameter</p>
     * <p>If the list of query parameter values is longer than {@code expectedValues}, all additional values will be ignored.</p>
     * <p>If {@code expectedValues} is longer than the list of query parameter values, an {@link AssertionError} will be thrown.</p>
     *
     * @param name           The name of the query parameter whose existence and value(s) to assert
     * @param expectedValues The expected values of the query parameter, in order.
     *                       <p>The n<sup>th</sup> expected value is compared to the n<sup>th</sup> query parameter value</p>
     */
    public static RequestMatcher queryParam(String name, String... expectedValues) {
        return request -> {
            MultivaluedMap<String, String> queryParams = getQueryParams(request.getUri());
            assertCount("QueryParam", name, queryParams, expectedValues.length);

            for (int i = 0; i < expectedValues.length; i++) {
                assertEqual("QueryParam [name=" + name + ", position=" + i + "]", expectedValues[i], queryParams.get(name).get(i));
            }
        };
    }

    /**
     * <p>Assert the values of a single request header</p>
     * <p>If the list of header values is longer than {@code expectedValues}, all additional values will be ignored.</p>
     * <p>If {@code expectedValues} is longer than the list of header values, an {@link AssertionError} will be thrown.</p>
     *
     * @param name           The name of the header whose existence and value(s) to assert
     * @param expectedValues The expected values of the header, in order.
     *                       <p>The n<sup>th</sup> expected value is compared to the n<sup>th</sup> header value</p>
     */
    public static RequestMatcher header(String name, String... expectedValues) {
        return request -> {
            assertCount("header", name, request.getStringHeaders(), expectedValues.length);

            List<String> actualValues = request.getStringHeaders().get(name);
            for (int i = 0; i < expectedValues.length; i++) {
                assertEqual("Request header [name=" + name + ", position=" + i + "]", expectedValues[i], actualValues.get(i));
            }
        };
    }

    /**
     * Assert that the given header is not present in the request.
     *
     * @param name The name of the header
     */
    public static RequestMatcher headerDoesNotExist(String name) {
        return request -> {
            List<Object> headerValues = request.getHeaders().get(name);
            if (headerValues != null) {
                throw new AssertionError("Expected header <" + name + "> to not exist, but it exists with values: " + headerValues);
            }
        };
    }

    /**
     * Access to request entity / body matchers.
     */
    public static EntityRequestMatchers entity() {
        return new EntityRequestMatchers();
    }

    /**
     * Access to request body matchers using a <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
     * to inspect a specific subset of the body.
     * <p>The JSON path expression can be parameterized using using formatting specifiers as defined in{@link String#format(String, Object...)}</p>
     *
     * @param expression The JSON path expression, possibly parameterized
     * @param args       Arguments to parameterize the JSON path expression with
     */
    public static JsonPathRequestMatchers jsonPath(String expression, Object... args) {
        return new JsonPathRequestMatchers(expression, args);
    }

    /**
     * Access to request body matchers using an {@link javax.xml.xpath.XPath XPath} expression
     * to inspect a specific subset of the body.
     * <p>The XPath path expression can be parameterized using using formatting specifiers as defined in{@link String#format(String, Object...)}</p>
     *
     * @param expression The XPath path expression, possibly parameterized
     * @param args       Arguments to parameterize the XPath path expression with
     * @throws XPathExpressionException On invalid XPath expressions
     */
    public static XpathRequestMatchers xpath(String expression, Object... args) throws XPathExpressionException {
        return new XpathRequestMatchers(expression, null, args);
    }

    /**
     * Access to request body matchers using a <b>namespace-aware</b> {@link javax.xml.xpath.XPath XPath} expression
     * to inspect a specific subset of the body.
     * <p>The XPath path expression can be parameterized using using formatting specifiers as defined in{@link String#format(String, Object...)}</p>
     *
     * @param expression The XPath path expression, possibly parameterized
     * @param namespaces The namespaces referenced in the XPath expression
     * @param args       Arguments to parameterize the XPath path expression with
     * @throws XPathExpressionException On invalid XPath expressions
     */
    public static XpathRequestMatchers xpath(String expression, Map<String, String> namespaces, Object... args) throws XPathExpressionException {
        return new XpathRequestMatchers(expression, namespaces, args);
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
            throw new AssertionError(message + " to exist but was null");
        }
        if (count > values.size()) {
            throw new AssertionError(message + " to have at least <" + count + "> values but found " + values);
        }
    }
}
