package io.github.solaris.jaxrs.client.test.response;

import io.github.solaris.jaxrs.client.test.request.RequestMatcher;

public interface ResponseActions {

    ResponseActions andExpect(RequestMatcher requestMatcher);

    void andRespond(ResponseCreator responseCreator);
}
