package io.github.solaris.jaxrs.client.test.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.Response;

import io.github.solaris.jaxrs.client.test.response.ResponseActions;
import io.github.solaris.jaxrs.client.test.response.ResponseCreator;
import org.jspecify.annotations.Nullable;

public class RequestExpectation implements RequestMatcher, ResponseActions, ResponseCreator {

    private int matchedCount;

    @Nullable
    private ResponseCreator responseCreator;

    private final List<RequestMatcher> matchers = new ArrayList<>();
    private final ExpectedCount expectedCount;

    public RequestExpectation(ExpectedCount expectedCount, RequestMatcher matcher) {
        this.expectedCount = expectedCount;
        matchers.add(matcher);
    }

    @Override
    public void match(ClientRequestContext request) throws IOException {
        for (RequestMatcher matcher : matchers) {
            matcher.match(request);
        }
    }

    @Override
    public ResponseActions andExpect(RequestMatcher requestMatcher) {
        matchers.add(requestMatcher);
        return this;
    }

    @Override
    public void andRespond(ResponseCreator responseCreator) {
        this.responseCreator = responseCreator;
    }

    public boolean hasRemainingCount() {
        return matchedCount < expectedCount.getMax();
    }

    public boolean isSatisfied() {
        return matchedCount >= expectedCount.getMin();
    }

    public void incrementAndValidate() {
        matchedCount++;
        if (matchedCount > expectedCount.getMax()) {
            throw new AssertionError("Received more calls than expected.");
        }
    }

    @Override
    public Response createResponse(ClientRequestContext request) throws IOException {
        if (responseCreator == null) {
            throw new IllegalStateException("Call to createResponse before responseCreator was set.");
        }
        return responseCreator.createResponse(request);
    }
}
