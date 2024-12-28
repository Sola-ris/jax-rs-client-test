package io.github.solaris.jaxrs.client.test.manager;

import java.io.IOException;
import java.util.Iterator;

import jakarta.ws.rs.client.ClientRequestContext;

import io.github.solaris.jaxrs.client.test.request.RequestExpectation;
import org.jspecify.annotations.Nullable;

public class OrderedRequestExpectationManager extends RequestExpectationManager {
    private final RequestExpectationGroup expectationGroup = new RequestExpectationGroup();

    @Nullable
    private Iterator<RequestExpectation> expectationIterator;

    @Override
    protected void expectationsDeclared() {
        expectationIterator = getExpectations().iterator();
    }

    @Override
    protected RequestExpectation matchRequest(ClientRequestContext requestContext) throws IOException {
        RequestExpectation expectation = expectationGroup.findExpectation(requestContext);
        if (expectation == null) {
            if (expectationIterator == null || !expectationIterator.hasNext()) {
                throw createUnexpectedRequestError(requestContext);
            }
            expectation = expectationIterator.next();
            expectation.match(requestContext);
        }
        expectationGroup.update(expectation);
        return expectation;
    }

    @Override
    public void reset() {
        super.reset();
        expectationIterator = null;
        expectationGroup.reset();
    }
}
