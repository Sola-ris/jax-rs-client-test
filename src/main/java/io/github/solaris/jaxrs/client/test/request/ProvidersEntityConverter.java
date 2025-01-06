package io.github.solaris.jaxrs.client.test.request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

import org.jspecify.annotations.Nullable;

/**
 * {@link EntityConverter} that directly uses the available JAX-RS {@link Providers} to convert the entity.
 * <p>Must not be directly instantiated, use {@link EntityConverter#fromRequestContext(ClientRequestContext)}.</p>
 */
public final class ProvidersEntityConverter extends EntityConverter {
    private static final Annotation[] ANNOTATIONS = new Annotation[]{};
    private final Providers providers;

    public ProvidersEntityConverter(Providers providers) {
        this.providers = providers;
    }

    @Override
    public <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) throws IOException {
        return convertEntity(requestContext, type, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) throws IOException {
        return convertEntity(requestContext, (Class<T>) genericType.getRawType(), genericType.getType());
    }

    @SuppressWarnings("unchecked")
    private <T> T convertEntity(ClientRequestContext requestContext, Class<T> type, @Nullable Type genericType) throws IOException {
        if (canShortCircuit(requestContext, type, genericType)) {
            return (T) requestContext.getEntity();
        }

        MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(
            requestContext.getEntityClass(),
            requestContext.getEntityType(),
            requestContext.getEntityAnnotations(),
            requestContext.getMediaType()
        );

        if (writer == null) {
            throw new ProcessingException("Unable to obtain MessageBodyWriter for type=" + type + " and genericType=" + genericType);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.writeTo(
            requestContext.getEntity(),
            requestContext.getEntityClass(),
            requestContext.getEntityType(),
            requestContext.getEntityAnnotations(),
            requestContext.getMediaType(),
            requestContext.getHeaders(),
            outputStream
        );

        MessageBodyReader<T> reader = providers.getMessageBodyReader(
            type,
            genericType,
            ANNOTATIONS,
            requestContext.getMediaType()
        );

        if (reader == null) {
            throw new ProcessingException("Unable to obtain MessageBodyReader for type=" + type + " and genericType=" + genericType);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return reader.readFrom(
            type,
            genericType,
            ANNOTATIONS,
            requestContext.getMediaType(),
            requestContext.getStringHeaders(),
            inputStream
        );
    }
}
