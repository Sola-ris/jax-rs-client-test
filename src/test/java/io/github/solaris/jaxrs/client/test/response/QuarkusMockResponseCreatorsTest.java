package io.github.solaris.jaxrs.client.test.response;

import io.github.solaris.jaxrs.client.test.util.extension.QuarkusTestFactory;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class QuarkusMockResponseCreatorsTest extends QuarkusTestFactory {

    @Override
    protected Object getTestInstance() {
        return new MockResponseCreatorsTest().new WithEntity();
    }
}
