package io.github.solaris.jaxrs.client.test.request;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.GenericType;

import org.jspecify.annotations.Nullable;

/**
 * Utility class for {@link RequestMatcher} implementations to convert a request {@code entity} into another type, e.g. from a POJO into a String.
 * <p>Can be obtained inside a {@code RequestMatcher} implementation via {@link #fromRequestContext(ClientRequestContext)}.</p>
 * <pre><code>
 *  RequestMatcher customMatcher = request -> {
 *      EntityConverter converter = EntityConverter.fromRequestContext(request);
 *      String jsonString = converter.convert(request, String.class);
 *      // assertions on the JSON String
 *  }
 *
 *  // Client and MockRestServer setup
 *
 *  server.expect(customMatcher).andRespond(MockResponseCreators.withSuccess());
 *
 *  // execute request
 * </code></pre>
 */
public abstract sealed class EntityConverter permits ClientEntityConverter, ProvidersEntityConverter {

    /**
     * Obtain the {@code EntityConverter} from the current {@link ClientRequestContext} while inside a {@code RequestMatcher}.
     * <p>Will throw an {@link IllegalStateException} when called outside of a {@code RequestMatcher}.</p>
     *
     * @param requestContext The current request
     * @return The {@code EntityConverter} instance
     */
    public static EntityConverter fromRequestContext(ClientRequestContext requestContext) {
        if (requestContext.getProperty(EntityConverter.class.getName()) instanceof EntityConverter entityConverter) {
            return entityConverter;
        }

        throw new IllegalStateException("Unable to obtain EntityConverter from RequestContext.");
    }

    /**
     * Obtain the entity from the current {@link ClientRequestContext} and convert it to the type.
     *
     * @param requestContext The current request
     * @param type           The target type
     * @return The converted entity
     * @throws IOException Tf an I/O error occurs during conversion
     */
    public abstract <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) throws IOException;

    /**
     * Obtain the entity from the current {@link ClientRequestContext} and convert it to the generic type.
     *
     * @param requestContext The current request
     * @param genericType    The target type
     * @return The converted entity
     * @throws IOException If an I/O error occurs during conversion
     */
    public abstract <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) throws IOException;

    static boolean canShortCircuit(ClientRequestContext requestContext, Class<?> type, @Nullable Type genericType) {
        if (genericType == null) {
            return type.isAssignableFrom(requestContext.getEntityClass());
        }
        return type.isAssignableFrom(requestContext.getEntityClass())
            && Objects.equals(requestContext.getEntityType(), genericType);
    }
}
