package io.github.solaris.jaxrs.client.test.response;

import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;
import org.jspecify.annotations.NullUnmarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

@NullUnmarked
class ExecutingResponseCreatorTest {
    private static final AssertableHandler HANDLER = new AssertableHandler();
    private static final URI REQUEST_URI = URI.create("http://localhost:8080/hello");
    private static final String REQUEST_BODY = "{\"hello\": true}";

    private static HttpServer httpServer;

    @BeforeAll
    static void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/hello", HANDLER);
        httpServer.setExecutor(null);
        httpServer.start();
    }

    @BeforeEach
    void resetHandler() {
        HANDLER.reset();
    }

    @AfterAll
    static void stopServer() {
        httpServer.stop(0);
    }

    @JaxRsVendorTest
    void testDefaultClient() {
        testResponseCreator(new ExecutingResponseCreator());
    }

    @JaxRsVendorTest
    void testCustomClient() {
        try (Client client = ClientBuilder.newClient()) {
            testResponseCreator(new ExecutingResponseCreator(client));
        }
    }

    @JaxRsVendorTest
    void testDefaultClientWithoutBody() {
        testResponseCreatorWithoutBody(new ExecutingResponseCreator());
    }

    @JaxRsVendorTest
    void testCustomClientWithoutBody() {
        try (Client client = ClientBuilder.newClient()) {
            testResponseCreatorWithoutBody(new ExecutingResponseCreator(client));
        }
    }

    private static void testResponseCreator(ExecutingResponseCreator responseCreator) {
        HeaderCaptor headerCaptor = new HeaderCaptor();
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer mockServer = MockRestServer.bindTo(builder).build();

        mockServer.expect(requestTo(REQUEST_URI)).andRespond(responseCreator);
        mockServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Client client = builder.build()) {
                Response serverResponse = client.target(REQUEST_URI)
                    .register(headerCaptor)
                    .request()
                    .header("X-Custom", "Custom-X")
                    .post(Entity.entity(REQUEST_BODY, TEXT_PLAIN));
                assertThat(serverResponse.getStatusInfo().toEnum()).isEqualTo(OK);
                serverResponse.close();

                Response mockResponse = client.target("/goodbye").request().get();
                assertThat(mockResponse.getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();

        mockServer.verify();

        // Implementations add / compute additional headers
        assertThat(HANDLER.headers).containsAllEntriesOf(headerCaptor.headers);
        assertThat(HANDLER.requestUri).isEqualTo(REQUEST_URI);
        assertThat(HANDLER.body).isEqualTo(REQUEST_BODY);
    }

    private static void testResponseCreatorWithoutBody(ExecutingResponseCreator responseCreator) {
        HeaderCaptor headerCaptor = new HeaderCaptor();
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer mockServer = MockRestServer.bindTo(builder).build();

        mockServer.expect(requestTo(REQUEST_URI)).andRespond(responseCreator);
        mockServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Client client = builder.build()) {
                Response serverResponse = client.target(REQUEST_URI)
                    .register(headerCaptor)
                    .request()
                    .header("X-Custom", "Custom-X")
                    .get();
                assertThat(serverResponse.getStatusInfo().toEnum()).isEqualTo(OK);
                serverResponse.close();

                Response mockResponse = client.target("/goodbye").request().get();
                assertThat(mockResponse.getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();

        mockServer.verify();

        // Implementations add / compute additional headers
        assertThat(HANDLER.headers).containsAllEntriesOf(headerCaptor.headers);
        assertThat(HANDLER.requestUri).isEqualTo(REQUEST_URI);
    }

    private static class AssertableHandler implements HttpHandler {
        private String body = null;
        private URI requestUri = null;
        private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
                headers.addAll(header.getKey().toLowerCase(), header.getValue());
            }
            requestUri = URI.create(
                "http://" + exchange.getLocalAddress().getHostName() + ":" + exchange.getLocalAddress().getPort() + exchange.getRequestURI());
            body = new String(exchange.getRequestBody().readAllBytes(), UTF_8);

            exchange.sendResponseHeaders(OK.getStatusCode(), 0);
            exchange.close();
        }

        private void reset() {
            body = null;
            requestUri = null;
            headers.clear();
        }
    }

    private static class HeaderCaptor implements ClientRequestFilter {
        private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        @Override
        public void filter(ClientRequestContext requestContext) {
            for (Map.Entry<String, List<String>> header : requestContext.getStringHeaders().entrySet()) {
                headers.addAll(header.getKey().toLowerCase(), header.getValue());
            }
        }
    }
}