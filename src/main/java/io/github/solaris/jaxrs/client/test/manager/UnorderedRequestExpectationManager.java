package io.github.solaris.jaxrs.client.test.manager;

import jakarta.ws.rs.client.ClientRequestContext;

import io.github.solaris.jaxrs.client.test.request.RequestExpectation;

public class UnorderedRequestExpectationManager extends RequestExpectationManager {
    private final RequestExpectationGroup expectationGroup = new RequestExpectationGroup();

    @Override
    protected void expectationsDeclared() {
        expectationGroup.addExpectations(getExpectations());
    }

    @Override
    protected RequestExpectation matchRequest(ClientRequestContext requestContext) {
        RequestExpectation expectation = expectationGroup.findExpectation(requestContext);
        if (expectation == null) {
            throw createUnexpectedRequestError(requestContext);
        }

        expectationGroup.update(expectation);
        return expectation;
    }

    @Override
    public void reset() {
        super.reset();
        expectationGroup.reset();
    }
}
