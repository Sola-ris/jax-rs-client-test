package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_SVG_XML;
import static jakarta.ws.rs.core.MediaType.APPLICATION_SVG_XML_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.MockClientRequestContext;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;

class EntityRequestMatchersTest {

    @JaxRsVendorTest
    void testMediaType() {
        assertThatCode(() -> RequestMatchers.entity().mediaType(APPLICATION_JSON_TYPE).match(new MockClientRequestContext(APPLICATION_JSON_TYPE)))
            .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testMediaType_notSet() {
        assertThatThrownBy(
            () -> RequestMatchers.entity()
                .mediaType(APPLICATION_SVG_XML_TYPE)
                .match(new MockClientRequestContext((MediaType) null)))
            .isInstanceOf(AssertionError.class)
            .hasMessage("MediaType was not set.");
    }

    @JaxRsVendorTest
    void testMediaType_noMatch() {
        assertThatThrownBy(
            () -> RequestMatchers.entity()
                .mediaType(APPLICATION_JSON_TYPE)
                .match(new MockClientRequestContext(APPLICATION_OCTET_STREAM_TYPE)))
            .isInstanceOf(AssertionError.class)
            .hasMessage("MediaType expected: <%s> but was: <%s>", APPLICATION_JSON_TYPE, APPLICATION_OCTET_STREAM_TYPE);
    }

    @JaxRsVendorTest
    void testMediaType_string() {
        assertThatCode(() -> RequestMatchers.entity().mediaType(APPLICATION_JSON).match(new MockClientRequestContext(APPLICATION_JSON_TYPE)))
            .doesNotThrowAnyException();
    }

    @JaxRsVendorTest
    void testMediaType_string_notSet() {
        assertThatThrownBy(
            () -> RequestMatchers.entity()
                .mediaType(APPLICATION_SVG_XML)
                .match(new MockClientRequestContext((MediaType) null)))
            .isInstanceOf(AssertionError.class)
            .hasMessage("MediaType was not set.");
    }

    @JaxRsVendorTest
    void testMediaType_string_noMatch() {
        assertThatThrownBy(
            () -> RequestMatchers.entity()
                .mediaType(APPLICATION_JSON)
                .match(new MockClientRequestContext(APPLICATION_OCTET_STREAM_TYPE)))
            .isInstanceOf(AssertionError.class)
            .hasMessage("MediaType expected: <%s> but was: <%s>", APPLICATION_JSON, APPLICATION_OCTET_STREAM);
    }

    @JaxRsVendorTest
    void testIsEqualTo() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Dto dto = new Dto(true);

        server.expect(RequestMatchers.entity().isEqualTo(dto)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> client.target("/hello").request().post(Entity.entity(dto, TEXT_PLAIN_TYPE)).close())
                .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testIsEqualTo_noMatch() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Dto dto = new Dto(true);

        server.expect(RequestMatchers.entity().isEqualTo(dto)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> client.target("/hello").request().post(Entity.entity(dto, TEXT_PLAIN_TYPE)).close())
                .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testString(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Dto dto = new Dto(true);
        Dto otherDto = new Dto(false);

        server.expect(RequestMatchers.entity().isEqualTo(dto)).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.entity(otherDto, TEXT_PLAIN_TYPE)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Entity expected: <%s> but was: <%s>", dto, otherDto);
        }
    }

    @JaxRsVendorTest
    void testString_noMatch(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Dto dto = new Dto(true);
        Dto otherDto = new Dto(false);

        server.expect(RequestMatchers.entity().string(dto.toString())).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.entity(otherDto.toString().getBytes(), TEXT_PLAIN_TYPE)).close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Entity String expected: <%s> but was: <%s>", dto.toString(), otherDto.toString());
        }
    }

    @JaxRsVendorTest
    void testForm() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");

        server.expect(RequestMatchers.entity().form(form)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> client.target("/hello").request(APPLICATION_JSON_TYPE).post(Entity.form(form)).close())
                .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testForm_noMatch(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");
        Form otherForm = new Form("sendoff", "goodbye");

        server.expect(RequestMatchers.entity().form(form)).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.form(otherForm))
                    .close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Form expected: <%s> but was: <%s>", form.asMap(), otherForm.asMap());
        }
    }

    @JaxRsVendorTest
    void testFormContains() {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form actualForm = new Form()
            .param("greeting", "hello")
            .param("greeting", "salutations")
            .param("sendoff", "goodbye")
            .param("sendoff", "farewell");

        Form subset = new Form()
            .param("greeting", "hello")
            .param("sendoff", "goodbye");

        server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> client.target("/hello").request().post(Entity.form(actualForm)).close())
                .doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testFormContains_subsetIsLarger(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form actualForm = new Form()
            .param("sendoff", "goodbye")
            .param("greeting", "hello");

        Form subset = new Form()
            .param("greeting", "hello")
            .param("sendoff", "goodbye")
            .param("question", "how are you?");

        server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.form(actualForm))
                    .close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected " + subset.asMap() + " to be smaller or the same size as " + actualForm.asMap());
        }
    }

    @JaxRsVendorTest
    void testFormContains_parameterNotInSubset(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form actualForm = new Form()
            .param("sendoff", "goodbye")
            .param("greeting", "hello");

        Form subset = new Form()
            .param("question", "how are you?");

        server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.form(actualForm))
                    .close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected " + actualForm.asMap() + " to contain parameter 'question'");
        }
    }

    @JaxRsVendorTest
    void testFormContains_subsetHasMoreValues(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form actualForm = new Form()
            .param("greeting", "hello");

        Form subset = new Form()
            .param("greeting", "hello")
            .param("greeting", "salutations");

        server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.form(actualForm))
                    .close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("Expected %s to be smaller or the same size as %s", subset.asMap().get("greeting"), actualForm.asMap().get("greeting"));
        }
    }

    @JaxRsVendorTest
    void testFormContains_subsetHasDifferentValue(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form actualForm = new Form()
            .param("greeting", "hello")
            .param("greeting", "salutations");

        Form subset = new Form()
            .param("greeting", "good morning");

        server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.form(actualForm))
                    .close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("FormParam [name=greeting, position=0] expected: <good morning> but was: <hello>");
        }
    }

    @JaxRsVendorTest
    void testFormContains_subsetHasDifferentOrder(FilterExceptionAssert filterExceptionAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form actualForm = new Form()
            .param("greeting", "hello")
            .param("greeting", "salutations")
            .param("greeting", "good morning");

        Form subset = new Form()
            .param("greeting", "salutations")
            .param("greeting", "hello");


        server.expect(RequestMatchers.entity().formContains(subset)).andRespond(withSuccess());

        try (client) {
            filterExceptionAssert.assertThatThrownBy(() -> client.target("/hello")
                    .request()
                    .post(Entity.form(actualForm))
                    .close())
                .isInstanceOf(AssertionError.class)
                .hasMessage("FormParam [name=greeting, position=0] expected: <salutations> but was: <hello>");
        }
    }

    private record Dto(boolean flag) {}
}