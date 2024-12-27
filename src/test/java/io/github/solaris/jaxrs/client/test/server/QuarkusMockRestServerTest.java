package io.github.solaris.jaxrs.client.test.server;

import io.quarkus.test.junit.QuarkusTest;

import io.github.solaris.jaxrs.client.test.util.extension.QuarkusTestFactory;

@QuarkusTest
public class QuarkusMockRestServerTest extends QuarkusTestFactory {

    @Override
    protected Object getTestInstance() {
        return new MockRestServerTest().new BindMicroProfileRestClientBuilder();
    }
}
