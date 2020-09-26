/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.lighty.codecs.XmlNodeConverter;
import io.lighty.netconf.device.requests.notification.NotificationPublishService;
import io.lighty.netconf.device.requests.notification.NotificationPublishServiceImpl;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.api.DOMSchemaServiceExtension;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextTree;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceServicesImpl implements NetconfDeviceServices {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceServicesImpl.class);

    private final Map<LogicalDatastoreType, DOMStore> datastores;
    private final SchemaContextHolder schemaContext;
    private final DOMNotificationRouter domNotificationRouter;
    private final DOMDataBroker domDataBroker;
    private final DataBroker dataBroker;
    private final NotificationService notificationService;
    private final NotificationPublishServiceImpl notificationPublishService;
    private final XmlNodeConverter xmlNodeConverter;

    public NetconfDeviceServicesImpl(final Collection<YangModuleInfo> moduleInfos,
        final NotificationPublishServiceImpl creator) {
        this.schemaContext = new SchemaContextHolder(moduleInfos);
        if (creator != null) {
            creator.setSchemaContext(this.schemaContext.schemaContext);
            creator.setCodec(this.schemaContext.bindingStreamCodecs);
        }
        this.notificationPublishService = creator;
        this.datastores = createDatastores();
        this.domNotificationRouter = DOMNotificationRouter.create(16);
        this.domDataBroker = createDOMDataBroker();
        this.dataBroker = createDataBroker();
        this.notificationService = new BindingDOMNotificationServiceAdapter(this.domNotificationRouter,
                this.schemaContext.bindingStreamCodecs);
        this.xmlNodeConverter = new XmlNodeConverter(getSchemaContext());
        for (final ListenerRegistration<? extends SchemaContextListener> listener
                : this.schemaContext.listeners.getRegistrations()) {
            listener.getInstance().onGlobalContextUpdated(this.schemaContext.schemaContext);
        }
    }

    @Override
    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    @Override
    public DOMDataBroker getDOMDataBroker() {
        return this.domDataBroker;
    }

    @Override
    public SchemaContext getSchemaContext() {
        return this.schemaContext.schemaContext;
    }

    @Override
    public SchemaNode getRootSchemaNode() {
        return DataSchemaContextTree.from(getSchemaContext()).getRoot().getDataSchemaNode();
    }

    @Override
    public BindingNormalizedNodeCodecRegistry getBindingToNormalizedNodeCodec() {
        return this.schemaContext.bindingStreamCodecs;
    }

    @Override
    public NotificationService getNotificationService() {
        return this.notificationService;
    }

    @Override
    public NotificationPublishService getNotificationPublishService() {
        return this.notificationPublishService;
    }

    @Override
    public XmlNodeConverter getXmlNodeConverter() {
        return this.xmlNodeConverter;
    }

    private DOMDataBroker createDOMDataBroker() {
        return new SerializedDOMDataBroker(this.datastores,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
    }

    private ListeningExecutorService getDataTreeChangeListenerExecutor() {
        return MoreExecutors.newDirectExecutorService();
    }

    private DataBroker createDataBroker() {
        return new BindingDOMDataBrokerAdapter(getDOMDataBroker(), this.schemaContext.bindingToNormalized);
    }

    private Map<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION, createConfigurationDatastore()).build();
    }

    private DOMStore createConfigurationDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", LogicalDatastoreType.CONFIGURATION,
                getDataTreeChangeListenerExecutor(),
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, false);
        this.schemaContext.registerSchemaContextListener(store);
        return store;
    }

    private DOMStore createOperationalDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", getDataTreeChangeListenerExecutor());
        this.schemaContext.registerSchemaContextListener(store);
        return store;
    }

    private static final class SchemaContextHolder implements DOMSchemaService, SchemaContextProvider {

        private final SchemaContext schemaContext;
        private final ListenerRegistry<SchemaContextListener> listeners;
        private final BindingNormalizedNodeCodecRegistry bindingStreamCodecs;
        private final BindingToNormalizedNodeCodec bindingToNormalized;
        private final ModuleInfoBackedContext moduleInfoBackedCntxt;

        private SchemaContextHolder(final Collection<YangModuleInfo> moduleInfos) {
            this.moduleInfoBackedCntxt = ModuleInfoBackedContext.create();
            this.schemaContext = getSchemaContext(moduleInfos);
            this.listeners = ListenerRegistry.create();
            this.bindingStreamCodecs = createBindingRegistry();
            final GeneratedClassLoadingStrategy loading = GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy();
            this.bindingToNormalized = new BindingToNormalizedNodeCodec(loading, this.bindingStreamCodecs);
            registerSchemaContextListener(this.bindingToNormalized);
        }

        @Override
        public SchemaContext getSchemaContext() {
            return this.schemaContext;
        }

        /**
         * Get the schemacontext from loaded modules on classpath.
         *
         * @param moduleInfos a list of Yang module Infos
         * @return SchemaContext a schema context
         */
        private SchemaContext getSchemaContext(final Collection<YangModuleInfo> moduleInfos) {
            this.moduleInfoBackedCntxt.addModuleInfos(moduleInfos);
            final Optional<? extends SchemaContext> tryToCreateSchemaContext =
                    this.moduleInfoBackedCntxt.tryToCreateSchemaContext();
            if (!tryToCreateSchemaContext.isPresent()) {
                LOG.error("Could not create the initial schema context. Schema context is empty");
                throw new IllegalStateException();
            }
            return tryToCreateSchemaContext.get();
        }

        @Override
        public SchemaContext getGlobalContext() {
            return this.schemaContext;
        }

        @Override
        public SchemaContext getSessionContext() {
            return this.schemaContext;
        }

        @Override
        public ListenerRegistration<SchemaContextListener> registerSchemaContextListener(
                final SchemaContextListener listener) {
            return this.listeners.register(listener);
        }

        /**
         * Creates binding registry.
         *
         * @return BindingNormalizedNodeCodecRegistry the resulting binding registry
         */
        private BindingNormalizedNodeCodecRegistry createBindingRegistry() {
            final BindingRuntimeContext bindingContext =
                BindingRuntimeContext.create(this.moduleInfoBackedCntxt, this.schemaContext);
            final BindingNormalizedNodeCodecRegistry bindingNormalizedNodeCodecRegistry =
                    new BindingNormalizedNodeCodecRegistry(bindingContext);
            bindingNormalizedNodeCodecRegistry.onBindingRuntimeContextUpdated(bindingContext);
            return bindingNormalizedNodeCodecRegistry;
        }

        @Override
        public ClassToInstanceMap<DOMSchemaServiceExtension> getExtensions() {
            return null;
        }
    }
}
