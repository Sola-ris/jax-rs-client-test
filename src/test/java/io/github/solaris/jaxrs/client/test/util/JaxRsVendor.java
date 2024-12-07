package io.github.solaris.jaxrs.client.test.util;

import java.util.List;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;

public enum JaxRsVendor {
    JERSEY(
        org.glassfish.jersey.internal.RuntimeDelegateImpl.class,
        JerseyClientBuilder.class
    ),
    RESTEASY(
        ResteasyProviderFactoryImpl.class,
        ResteasyClientBuilderImpl.class
    ),
    CXF(
        org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl.class,
        org.apache.cxf.jaxrs.client.spec.ClientBuilderImpl.class
    ),
    RESTEASY_REACTIVE(
        org.jboss.resteasy.reactive.common.jaxrs.RuntimeDelegateImpl.class,
        org.jboss.resteasy.reactive.client.impl.ClientBuilderImpl.class
    );

    public static final List<JaxRsVendor> VENDORS = List.of(JaxRsVendor.values());

    private final Class<? extends RuntimeDelegate> runtimeDelegateClass;
    private final Class<? extends ClientBuilder> clientBuilderClass;
    private final VendorClassLoader vendorClassLoader;


    JaxRsVendor(Class<? extends RuntimeDelegate> runtimeDelegateClass, Class<? extends ClientBuilder> clientBuilderClass) {
        this.runtimeDelegateClass = runtimeDelegateClass;
        this.clientBuilderClass = clientBuilderClass;

        this.vendorClassLoader = new VendorClassLoader(this);
    }

    Class<? extends ClientBuilder> getClientBuilderClass() {
        return clientBuilderClass;
    }

    Class<? extends RuntimeDelegate> getRuntimeDelegateClass() {
        return runtimeDelegateClass;
    }

    public ClassLoader getVendorClassLoader() {
        return vendorClassLoader;
    }
}
