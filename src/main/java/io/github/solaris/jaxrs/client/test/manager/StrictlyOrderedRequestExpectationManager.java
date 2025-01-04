package io.github.solaris.jaxrs.client.test.manager;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;

import io.github.solaris.jaxrs.client.test.request.RequestExpectation;

public class StrictlyOrderedRequestExpectationManager extends RequestExpectationManager {

    @Override
    protected void expectationsDeclared() {}

    @Override
    protected RequestExpectation matchRequest(ClientRequestContext requestContext) throws IOException {
        RequestExpectation matchingExpectation = null;
        for (RequestExpectation expectation : getExpectations()) {
            if (expectation.isSatisfied()) {
                try {
                    expectation.match(requestContext);
                    matchingExpectation = expectation;
                    break;
                } catch (AssertionError ignore) {}
            } else {
                expectation.match(requestContext);
                matchingExpectation = expectation;
                break;
            }
        }

        if (matchingExpectation == null) {
            throw createUnexpectedRequestError(requestContext);
        }

        matchingExpectation.incrementAndValidate();
        return matchingExpectation;
    }
}
