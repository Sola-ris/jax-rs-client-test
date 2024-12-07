package io.github.solaris.jaxrs.client.test.server;

import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withException;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;
import org.assertj.core.api.Assertions;

public class MockRestServerTest {

    @JaxRsVendorTest
    void testOrderedExpectations() {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());
        server.expect(requestTo("/goodbye")).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Client client = builder.build()) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();

        server.verify();
    }

    @JaxRsVendorTest
    void testUnorderedExpectations() {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).ignoreRequestOrder(true).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());
        server.expect(requestTo("/goodbye")).andRespond(withSuccess());

        assertThatCode(() -> {
            try (Client client = builder.build()) {
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }
        }).doesNotThrowAnyException();

        server.verify();
    }

    @JaxRsVendorTest
    void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());
        server.expect(requestTo("/goodbye")).andRespond(withSuccess());

        try (Client client = builder.build()) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                .isInstanceOf(AssertionError.class)
                .hasMessageEndingWith("Unexpected Request. Expected: </hello> but was: </goodbye>");
        }
    }

    @JaxRsVendorTest
    void testReset() {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());

        try (Client client = builder.build()) {
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }
    }

    @JaxRsVendorTest
    void testUnsatisfiedExpectation() {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());
        server.expect(requestTo("/goodbye")).andRespond(withSuccess());

        try (Client client = builder.build()) {
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Assertions.assertThatThrownBy(server::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessageMatching("""
                    Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                    1 request\\(s\\) executed:
                    GET /hello.*$
                    """);
        }
    }

    @JaxRsVendorTest
    void testMultipleBuilds() {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        MockRestServerBuilder serverBuilder = MockRestServer.bindTo(clientBuilder);

        MockRestServer server = serverBuilder.build();
        server.expect(requestTo("/hello")).andRespond(withSuccess());
        try (Client client = clientBuilder.build()) {
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }

        server = serverBuilder.ignoreRequestOrder(true).build();
        server.expect(requestTo("/hello")).andRespond(withSuccess());
        server.expect(requestTo("/goodbye")).andRespond(withSuccess());

        try (Client client = clientBuilder.build()) {
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }

        server = serverBuilder.build();
        server.expect(requestTo("/goodbye")).andRespond(withSuccess());
        try (Client client = clientBuilder.build()) {
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }
    }

    @JaxRsVendorTest
    void testVerifyWithTimeout() {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());
        server.expect(requestTo("/goodbye")).andRespond(withSuccess());

        Duration verifyDuration = Duration.ofMillis(200L);
        try (Client client = builder.build()) {
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Instant start = Instant.now();
            Assertions.assertThatThrownBy(() -> server.verify(verifyDuration))
                .isInstanceOf(AssertionError.class)
                .hasMessageMatching("""
                    Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                    1 request\\(s\\) executed:
                    GET /hello.*$
                    """);
            assertThat(Duration.between(start, Instant.now()))
                .isGreaterThan(verifyDuration);
        }

        MockRestServer otherServer = MockRestServer.bindTo(builder).build();
        otherServer.expect(requestTo("/hello")).andRespond(withSuccess().entity("hello"));
        otherServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

        try (Client client = builder.build()) {
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            Instant start = Instant.now();
            assertThatCode(() -> otherServer.verify(verifyDuration))
                .doesNotThrowAnyException();
            assertThat(Duration.between(start, Instant.now()))
                .isLessThan(verifyDuration);
        }
    }

    @JaxRsVendorTest
    void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());

        try (Client client = builder.build()) {
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("No further requests expected");
        }

        assertThatThrownBy(server::verify)
            .isInstanceOf(AssertionError.class)
            .hasMessageStartingWith("Some requests did not execute successfully.");
    }

    @JaxRsVendorTest
    void testFailuresClearedAfterReset(FilterExceptionAssert filterExceptionAssert) {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/hello")).andRespond(withSuccess());

        try (Client client = builder.build()) {
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("No further requests expected");

            server.reset();

            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();
        }
    }

    @JaxRsVendorTest
    void testFollowUpRequestAfterFailure(FilterExceptionAssert filterExceptionAssert) {
        ClientBuilder builder = ClientBuilder.newBuilder();
        MockRestServer server = MockRestServer.bindTo(builder).build();

        server.expect(requestTo("/error")).andRespond(withException(new SocketException("Connection Reset")));
        server.expect(requestTo("/hello")).andRespond(withSuccess());

        try (Client client = builder.build()) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/error").request().get())
                .isInstanceOf(SocketException.class)
                .hasMessage("Connection Reset");

            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            server.verify();
        }
    }
}
