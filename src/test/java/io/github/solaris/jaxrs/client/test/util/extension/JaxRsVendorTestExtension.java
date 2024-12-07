package io.github.solaris.jaxrs.client.test.util.extension;

import static io.github.solaris.jaxrs.client.test.util.JaxRsVendor.CXF;

import java.lang.reflect.Method;

import jakarta.ws.rs.ext.RuntimeDelegate;

import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.CxfFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.FilterExceptionAssert.DefaultFilterExceptionAssert;
import io.github.solaris.jaxrs.client.test.util.JaxRsVendor;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

class JaxRsVendorTestExtension implements InvocationInterceptor, ParameterResolver {
    private final JaxRsVendor vendor;

    JaxRsVendorTestExtension(JaxRsVendor vendor) {
        this.vendor = vendor;
    }

    @Override
    public void interceptTestTemplateMethod(
        Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            RuntimeDelegate.setInstance(null);
            Thread.currentThread().setContextClassLoader(vendor.getVendorClassLoader());

            invocation.proceed();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return FilterExceptionAssert.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (vendor == CXF) {
            return new CxfFilterExceptionAssert();
        }
        return new DefaultFilterExceptionAssert();
    }
}
