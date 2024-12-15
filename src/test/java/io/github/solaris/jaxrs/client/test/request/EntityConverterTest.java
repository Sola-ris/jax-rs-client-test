package io.github.solaris.jaxrs.client.test.request;

import static io.github.solaris.jaxrs.client.test.response.MockResponseCreators.withSuccess;
import static jakarta.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.server.MockRestServer;
import io.github.solaris.jaxrs.client.test.util.EntityConverterAssert;
import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;

class EntityConverterTest {

    @JaxRsVendorTest
    void testConvertEntity_type(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        String entity = "hello";

        server.expect(converterAssert.typeAsserter(entity, 1)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.text(entity.getBytes()))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testConvertEntity_type_shortCircuit(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        String entity = "hello";

        server.expect(converterAssert.typeAsserter(entity, 0)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.text(entity))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testConvertEntity_genericType(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");

        server.expect(converterAssert.genericTypeAsserter(form.asMap(), 1)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.form(form))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testConvertEntity_genericType_shortCircuit(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");
        GenericEntity<MultivaluedMap<String, String>> genericMap = new GenericEntity<>(form.asMap()) {};

        server.expect(converterAssert.genericTypeAsserter(form.asMap(), 0)).andRespond(withSuccess());

        try (client) {
            assertThatCode(() -> {
                try (Response response = client.target("/hello").request().post(Entity.entity(genericMap, APPLICATION_FORM_URLENCODED_TYPE))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
                }
            }).doesNotThrowAnyException();
        }
    }

    @JaxRsVendorTest
    void testUnableToConvertEntity_writingFails(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        Form form = new Form("greeting", "hello");

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, Dto.class);
        }).andRespond(withSuccess());

        try (client) {
            converterAssert.assertConversionFailure(() -> client.target("/hello").request().post(Entity.form(form)).close());
        }
    }

    @JaxRsVendorTest
    void testUnableToConvertEntity_readingFails(EntityConverterAssert converterAssert) {
        Client client = ClientBuilder.newClient();
        MockRestServer server = MockRestServer.bindTo(client).build();

        server.expect(request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            converter.convertEntity(request, Dto.class);
        }).andRespond(withSuccess());

        try (client) {
            converterAssert.assertConversionFailure(() -> client.target("/hello")
                .request()
                .post(Entity.entity(Class.class, TEXT_HTML_TYPE))
                .close());
        }
    }

    private record Dto(boolean flag) {}
}