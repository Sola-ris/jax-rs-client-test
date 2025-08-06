package io.github.solaris.jaxrs.client.test.util;

import static org.apache.cxf.jaxrs.provider.ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION;

import java.util.function.Supplier;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

public sealed interface ConfiguredClientSupplier extends Supplier<Client> {

    final class DefaultClientSupplier implements ConfiguredClientSupplier {

        @Override
        public Client get() {
            return ClientBuilder.newClient();
        }
    }

    final class CxfClientSupplier implements ConfiguredClientSupplier {

        @Override
        public Client get() {
            return ClientBuilder.newBuilder()
                    .register(new JacksonJsonProvider())
                    .build();
        }

        public static class ConfiguredBusFactory extends CXFBusFactory {

            @Override
            public Bus createBus() {
                Bus bus = super.createBus();
                bus.setProperty(SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
                return bus;
            }
        }
    }
}
