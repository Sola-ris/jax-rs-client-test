package io.github.solaris.jaxrs.client.test.request;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

public final class ProvidersEntityConverter extends EntityConverter {
    private static final Annotation[] ANNOTATIONS = new Annotation[]{};
    private final Providers providers;

    public ProvidersEntityConverter(Providers providers) {
        this.providers = providers;
    }

    @Override
    public <T> T convertEntity(ClientRequestContext requestContext, Class<T> type) {
        return convertEntity(requestContext, type, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T convertEntity(ClientRequestContext requestContext, GenericType<T> genericType) {
        return convertEntity(requestContext, (Class<T>) genericType.getRawType(), genericType.getType());
    }

    @SuppressWarnings("unchecked")
    private <T> T convertEntity(ClientRequestContext requestContext, Class<T> type, Type genericType) {
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
            throw new IllegalStateException("Unable to obtain MessageBodyWriter for type=" + type + " and genericType=" + genericType);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            writer.writeTo(
                requestContext.getEntity(),
                requestContext.getEntityClass(),
                requestContext.getEntityType(),
                requestContext.getEntityAnnotations(),
                requestContext.getMediaType(),
                requestContext.getHeaders(),
                outputStream
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        MessageBodyReader<T> reader = providers.getMessageBodyReader(
            type,
            genericType,
            ANNOTATIONS,
            requestContext.getMediaType()
        );

        if (reader == null) {
            throw new IllegalStateException("Unable to obtain MessageBodyReader for type=" + type + " and genericType=" + genericType);
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try {
            return reader.readFrom(
                type,
                genericType,
                ANNOTATIONS,
                requestContext.getMediaType(),
                requestContext.getStringHeaders(),
                inputStream
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
