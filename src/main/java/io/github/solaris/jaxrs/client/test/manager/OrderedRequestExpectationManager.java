package io.github.solaris.jaxrs.client.test.manager;

import java.util.Iterator;

import jakarta.ws.rs.client.ClientRequestContext;

import io.github.solaris.jaxrs.client.test.request.RequestExpectation;

public class OrderedRequestExpectationManager extends RequestExpectationManager {
    private final RequestExpectationGroup expectationGroup = new RequestExpectationGroup();

    private Iterator<RequestExpectation> expectationIterator;

    @Override
    protected void expectationsDeclared() {
        expectationIterator = getExpectations().iterator();
    }

    @Override
    protected RequestExpectation matchRequest(ClientRequestContext requestContext) {
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
