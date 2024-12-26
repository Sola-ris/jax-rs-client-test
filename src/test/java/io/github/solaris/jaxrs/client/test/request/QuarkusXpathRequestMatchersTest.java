package io.github.solaris.jaxrs.client.test.request;

import io.quarkus.test.junit.QuarkusTest;

import io.github.solaris.jaxrs.client.test.util.extension.QuarkusTestFactory;

@QuarkusTest
public class QuarkusXpathRequestMatchersTest extends QuarkusTestFactory {

    @Override
    protected Object getTestInstance() {
        return new XpathRequestMatchersTest();
    }
}
