package io.github.solaris.jaxrs.client.test.request;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.GenericType;

import org.jspecify.annotations.Nullable;

public abstract sealed class EntityConverter permits ClientEntityConverter, ProvidersEntityConverter {

    public static EntityConverter fromRequestContext(ClientRequestContext requestContext) {
        if (requestContext.getProperty(EntityConverter.class.getName()) instanceof EntityConverter entityConverter) {
            return entityConverter;
        }

        throw new IllegalStateException("Unable to obtain EntityConverter from RequestContext.");
    }

    public abstract <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) throws IOException;

    public abstract <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) throws IOException;

    static boolean canShortCircuit(ClientRequestContext requestContext, Class<?> type, @Nullable Type genericType) {
        if (genericType == null) {
            return type.isAssignableFrom(requestContext.getEntityClass());
        }
        return type.isAssignableFrom(requestContext.getEntityClass())
            && Objects.equals(requestContext.getEntityType(), genericType);
    }
}
