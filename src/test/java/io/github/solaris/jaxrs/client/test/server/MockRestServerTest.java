package io.github.solaris.jaxrs.client.test.server;

import static io.github.solaris.jaxrs.client.test.request.ExpectedCount.min;
import static io.github.solaris.jaxrs.client.test.request.RequestMatchers.requestTo;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withException;
import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static io.github.solaris.jaxrs.client.test.util.JaxRsVendor.CXF;
import static io.github.solaris.jaxrs.client.test.util.JaxRsVendor.RESTEASY_REACTIVE;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.SocketException;
import java.time.Duration;
import java.time.Instant;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.GreetingSendoffClient;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;
import io.github.solaris.jaxrs.client.test.util.extension.RunInQuarkus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;

public class MockRestServerTest {

    @Nested
    class BindClientBuilder {

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
                    .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
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

        @JaxRsVendorTest(skipFor = RESTEASY_REACTIVE) // ClientBuilder::build currently passes the Configuration instead of cloning it
        void testBuilderUnaffectedByBoundClient() {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(min(1), requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }

            try (Client otherClient = builder.build()) {
                assertThatThrownBy(() -> otherClient.target("/hello").request().get())
                    .isInstanceOf(ProcessingException.class);
            }

            server.verify();
        }

        @JaxRsVendorTest
        void testBuilderUnaffectedByBoundWebTarget() {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(min(1), requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            }

            try (Client otherClient = builder.build()) {
                assertThatThrownBy(() -> otherClient.target("/hello").request().get())
                    .isInstanceOf(ProcessingException.class);
            }

            server.verify();
        }
    }

    @Nested
    class BindClient {

        @JaxRsVendorTest
        void testOrderedExpectations() {
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (client) {
                    assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testUnorderedExpectations() {
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).ignoreRequestOrder(true).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (client) {
                    assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest
        void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) {
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
                filterExceptionAssert.assertThatThrownBy(() -> client.target("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
            }
        }

        @JaxRsVendorTest
        void testReset() {
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
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
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
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
            Client client = ClientBuilder.newClient();
            MockRestServerBuilder serverBuilder = MockRestServer.bindTo(client);

            MockRestServer server = serverBuilder.build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.ignoreRequestOrder(true).build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.build();
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            try (client) {
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest
        void testVerifyWithTimeout() {
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            Duration verifyDuration = Duration.ofMillis(200L);
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

            MockRestServer otherServer = MockRestServer.bindTo(client).build();
            otherServer.expect(requestTo("/hello")).andRespond(withSuccess().entity("hello"));
            otherServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.target("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant otherStart = Instant.now();
                assertThatCode(() -> otherServer.verify(verifyDuration))
                    .doesNotThrowAnyException();
                assertThat(Duration.between(otherStart, Instant.now()))
                    .isLessThan(verifyDuration);
            }
        }

        @JaxRsVendorTest
        void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) {
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
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
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
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
            Client client = ClientBuilder.newClient();
            MockRestServer server = MockRestServer.bindTo(client).build();

            server.expect(requestTo("/error")).andRespond(withException(new SocketException("Connection Reset")));
            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                filterExceptionAssert.assertThatThrownBy(() -> client.target("/error").request().get())
                    .isInstanceOf(SocketException.class)
                    .hasMessage("Connection Reset");

                assertThat(client.target("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testClientUnaffectedByBoundWebTarget() {
            Client client = ClientBuilder.newBuilder().build();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(min(1), requestTo("/hello")).andRespond(withSuccess());

            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

            try (client) {
                assertThatThrownBy(() -> client.target("/hello").request().get())
                    .isInstanceOf(ProcessingException.class);
            }

            server.verify();
        }
    }

    @Nested
    class BindWebTarget {

        @JaxRsVendorTest
        void testOrderedExpectations() {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
                assertThatCode(() -> {
                    assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                }).doesNotThrowAnyException();
            }

            server.verify();
        }

        @JaxRsVendorTest
        void testUnorderedExpectations() {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).ignoreRequestOrder(true).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
                assertThatCode(() -> {
                    assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                }).doesNotThrowAnyException();
            }

            server.verify();
        }

        @JaxRsVendorTest
        void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
                filterExceptionAssert.assertThatThrownBy(() -> target.path("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageEndingWith("Unexpected Request. expected: </hello> but was: </goodbye>");
            }
        }

        @JaxRsVendorTest
        void testReset() {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                server.expect(requestTo("/goodbye")).andRespond(withSuccess());
                assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest
        void testUnsatisfiedExpectation() {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

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
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServerBuilder serverBuilder = MockRestServer.bindTo(target);

            MockRestServer server = serverBuilder.build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.ignoreRequestOrder(true).build();
            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
            server.verify();

            server = serverBuilder.build();
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());
            try (client) {
                assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest
        void testVerifyWithTimeout() {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());
            server.expect(requestTo("/goodbye")).andRespond(withSuccess());

            Duration verifyDuration = Duration.ofMillis(200L);
            assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

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

            MockRestServer otherServer = MockRestServer.bindTo(target).build();
            otherServer.expect(requestTo("/hello")).andRespond(withSuccess().entity("hello"));
            otherServer.expect(requestTo("/goodbye")).andRespond(withSuccess());

            try (client) {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant otherStart = Instant.now();
                assertThatCode(() -> otherServer.verify(verifyDuration))
                    .doesNotThrowAnyException();
                assertThat(Duration.between(otherStart, Instant.now()))
                    .isLessThan(verifyDuration);
            }
        }

        @JaxRsVendorTest
        void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                filterExceptionAssert.assertThatThrownBy(() -> target.path("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");
            }

            assertThatThrownBy(server::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Some requests did not execute successfully.");
        }

        @JaxRsVendorTest
        void testFailuresClearedAfterReset(FilterExceptionAssert filterExceptionAssert) {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();

                filterExceptionAssert.assertThatThrownBy(() -> target.path("/goodbye").request().get())
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");

                server.reset();

                server.expect(requestTo("/goodbye")).andRespond(withSuccess());

                assertThat(target.path("/goodbye").request().get().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest
        void testFollowUpRequestAfterFailure(FilterExceptionAssert filterExceptionAssert) {
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target("");
            MockRestServer server = MockRestServer.bindTo(target).build();

            server.expect(requestTo("/error")).andRespond(withException(new SocketException("Connection Reset")));
            server.expect(requestTo("/hello")).andRespond(withSuccess());

            try (client) {
                filterExceptionAssert.assertThatThrownBy(() -> target.path("/error").request().get())
                    .isInstanceOf(SocketException.class)
                    .hasMessage("Connection Reset");

                assertThat(target.path("/hello").request().get().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }
    }

    @Nested
    @RunInQuarkus
    class BindMicroProfileRestClientBuilder {

        @JaxRsVendorTest(skipFor = CXF)
        void testOrderedExpectations() {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                    assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testUnorderedExpectations() {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).ignoreRequestOrder(true).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            assertThatCode(() -> {
                try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                    assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                    assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();

            server.verify();
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testOrderedExpectations_requestsOutOfOrder(FilterExceptionAssert filterExceptionAssert) throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageEndingWith("Unexpected Request. expected: <http://localhost/hello> but was: <http://localhost/goodbye>");
            }
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testReset() throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
                server.reset();

                server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testUnsatisfiedExpectation() throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                Assertions.assertThatThrownBy(server::verify)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageMatching("""
                        Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                        1 request\\(s\\) executed:
                        GET http://localhost/hello.*$
                        """);
            }
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testMultipleBuilds() throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServerBuilder serverBuilder = MockRestServer.bindTo(restClientBuilder);

            MockRestServer server = serverBuilder.build();
            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.ignoreRequestOrder(true).build();
            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }

            server = serverBuilder.build();
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());
            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testVerifyWithTimeout() throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());
            server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            Duration verifyDuration = Duration.ofMillis(200L);
            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant start = Instant.now();
                Assertions.assertThatThrownBy(() -> server.verify(verifyDuration))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageMatching("""
                        Further request\\(s\\) expected leaving 1 unsatisfied expectation\\(s\\)\\.
                        1 request\\(s\\) executed:
                        GET http://localhost/hello.*$
                        """);
                assertThat(Duration.between(start, Instant.now()))
                    .isGreaterThan(verifyDuration);
            }

            MockRestServer otherServer = MockRestServer.bindTo(restClientBuilder).build();
            otherServer.expect(requestTo("http://localhost/hello")).andRespond(withSuccess().entity("hello"));
            otherServer.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);

                Instant start = Instant.now();
                assertThatCode(() -> otherServer.verify(verifyDuration))
                    .doesNotThrowAnyException();
                assertThat(Duration.between(start, Instant.now()))
                    .isLessThan(verifyDuration);
            }
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testVerifyFailsAfterRequestFailure(FilterExceptionAssert filterExceptionAssert) throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");
            }

            assertThatThrownBy(server::verify)
                .isInstanceOf(AssertionError.class)
                .hasMessageStartingWith("Some requests did not execute successfully.");
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testFailuresClearedAfterReset(FilterExceptionAssert filterExceptionAssert) throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();

                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                    .isInstanceOf(AssertionError.class)
                    .hasMessageStartingWith("No further requests expected");

                server.reset();

                server.expect(requestTo("http://localhost/goodbye")).andRespond(withSuccess());

                assertThat(client.sendoff().getStatusInfo().toEnum()).isEqualTo(OK);
                server.verify();
            }
        }

        @JaxRsVendorTest(skipFor = CXF)
        void testFollowUpRequestAfterFailure(FilterExceptionAssert filterExceptionAssert) throws Exception {
            RestClientBuilder restClientBuilder = RestClientBuilder.newBuilder().baseUri("http://localhost");
            MockRestServer server = MockRestServer.bindTo(restClientBuilder).build();

            server.expect(requestTo("http://localhost/goodbye")).andRespond(withException(new SocketException("Connection Reset")));
            server.expect(requestTo("http://localhost/hello")).andRespond(withSuccess());

            try (GreetingSendoffClient client = restClientBuilder.build(GreetingSendoffClient.class)) {
                filterExceptionAssert.assertThatThrownBy(client::sendoff)
                    .isInstanceOf(SocketException.class)
                    .hasMessage("Connection Reset");

                assertThat(client.greeting().getStatusInfo().toEnum()).isEqualTo(OK);

                server.verify();
            }
        }
    }
}
