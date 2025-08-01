package io.github.solaris.jaxrs.client.test.request;

import org.jspecify.annotations.Nullable;

/**
 * Variant of {@link java.util.function.Consumer Consumer} that can throw an {@link Exception}.
 *
 * @param <T> the type of the consumed argument
 */
@FunctionalInterface
public interface ThrowingConsumer<T extends @Nullable Object> {

    /**
     * Consume the supplied argument, while potentially throwing an exception.
     *
     * @param t The argument to consume
     */
    void accept(@Nullable T t) throws Exception;
    // Should be inferred from ThrowingConsumer<T extends @Nullable Object>
    // but NullAway does not at the moment, while IntelliJ does
    // Could be https://github.com/uber/NullAway/issues/1155 or https://github.com/uber/NullAway/issues/1075
}
