package io.github.solaris.jaxrs.client.test.request;

import io.quarkus.test.junit.QuarkusTest;

import io.github.solaris.jaxrs.client.test.util.extension.QuarkusTestFactory;

@QuarkusTest
class QuarkusJsonPathRequestMatchersTest extends QuarkusTestFactory {

    @Override
    protected Object getTestInstance() {
        return new JsonPathRequestMatchersTest();
    }
}
