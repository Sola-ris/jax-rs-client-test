package io.github.solaris.jaxrs.client.test.internal;

import java.util.Objects;

public class Assertions {
    private Assertions() {}

    public static void fail(String message) {
        throw new AssertionError(message);
    }

    public static void assertEqual(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            fail(message + " expected: <" + expected + "> but was: <" + actual + ">");
        }
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) {
            fail(message);
        }
    }
}
