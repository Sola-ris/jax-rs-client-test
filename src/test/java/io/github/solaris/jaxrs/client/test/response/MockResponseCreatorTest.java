package io.github.solaris.jaxrs.client.test.response;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_ENCODING;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import static jakarta.ws.rs.core.NewCookie.SameSite.NONE;
import static jakarta.ws.rs.core.NewCookie.SameSite.STRICT;
import static jakarta.ws.rs.core.Response.Status.OK;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Year;
import java.util.Date;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.util.extension.JaxRsVendorTest;

class MockResponseCreatorTest {

    @JaxRsVendorTest
    void testResponseWithStatus() {
        try (Response response = new MockResponseCreator(OK).createResponse(null)) {
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(OK);
        }
    }

    @JaxRsVendorTest
    void testResponseWithMediaType() {
        try (Response response = new MockResponseCreator(OK).mediaType(APPLICATION_JSON_TYPE).createResponse(null)) {
            assertThat(response.getMediaType()).isEqualTo(APPLICATION_JSON_TYPE);
        }
    }

    @JaxRsVendorTest
    void testResponseWithHeaders() {
        try (Response response =
                 new MockResponseCreator(OK)
                     .header(ACCEPT_ENCODING, "gzip", "deflate", "br")
                     .header(ACCEPT, WILDCARD)
                     .createResponse(null)) {
            assertThat(response.getHeaders()).satisfies(
                headers -> assertThat(headers.get(ACCEPT_ENCODING)).containsExactlyInAnyOrder("gzip", "deflate", "br"),
                headers -> assertThat(headers.get(ACCEPT)).singleElement().isEqualTo(WILDCARD)
            );
        }
    }

    @JaxRsVendorTest
    void testRespondWithEntity() {
        String json = "{\"foo\": true}";
        try (Response response = new MockResponseCreator(OK).entity(json).createResponse(null)) {
            assertThat(response.getEntity()).isEqualTo(json);
        }
    }

    @JaxRsVendorTest
    void testRespondWithCookies() {
        NewCookie sessionCookie = new NewCookie.Builder("session-token")
            .maxAge(-1)
            .comment("top-secret")
            .value("123456")
            .sameSite(STRICT)
            .secure(true)
            .version(42)
            .build();
        NewCookie themeCookie = new NewCookie.Builder("theme")
            // Truncated to seconds to prevent differences in millis after parsing
            .expiry(Date.from(Instant.now().plus(Year.now().length(), DAYS).truncatedTo(SECONDS)))
            .maxAge(Long.valueOf(YEARS.getDuration().getSeconds()).intValue())
            .secure(false)
            .sameSite(NONE)
            .value("dark")
            .build();

        try (Response response =
                 new MockResponseCreator(OK)
                     .cookies(sessionCookie, themeCookie)
                     .createResponse(null)) {
            assertThat(response.getCookies()).satisfies(
                cookies -> assertThat(cookies.get("session-token")).isEqualTo(sessionCookie),
                cookies -> assertThat(cookies.get("theme")).isEqualTo(themeCookie)
            );
        }
    }
}