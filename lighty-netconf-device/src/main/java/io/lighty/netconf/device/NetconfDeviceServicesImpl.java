/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.lighty.codecs.util.XmlNodeConverter;
import io.lighty.netconf.device.requests.notification.NotificationPublishService;
import io.lighty.netconf.device.requests.notification.NotificationPublishServiceImpl;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.AdapterContext;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMDataBrokerAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.BindingDOMNotificationServiceAdapter;
import org.opendaylight.mdsal.binding.dom.adapter.ConstantAdapterContext;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.generator.impl.DefaultBindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeTypes;
import org.opendaylight.mdsal.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.api.ModuleInfoSnapshot;
import org.opendaylight.mdsal.binding.runtime.spi.ModuleInfoSnapshotResolver;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack.Inference;
import org.opendaylight.yangtools.yang.parser.api.YangParserFactory;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceServicesImpl implements NetconfDeviceServices {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceServicesImpl.class);

    private final Map<LogicalDatastoreType, DOMStore> datastores;
    private final AdapterContext adapterContext;
    private final EffectiveModelContext effectiveModelContext;
    private final DOMNotificationRouter domNotificationRouter;
    private final DOMDataBroker domDataBroker;
    private final DataBroker dataBroker;
    private final NotificationService notificationService;
    private final NotificationPublishServiceImpl notificationPublishService;
    private final XmlNodeConverter xmlNodeConverter;

    public NetconfDeviceServicesImpl(
        final Collection<YangModuleInfo> moduleInfos, final NotificationPublishServiceImpl creator) {
        this.adapterContext = createAdapterContext(moduleInfos);
        this.effectiveModelContext = adapterContext.currentSerializer().getRuntimeContext().getEffectiveModelContext();

        if (creator != null) {
            creator.setAdapterContext(this.adapterContext);
        }

        this.notificationPublishService = creator;
        this.datastores = createDatastores();
        this.domNotificationRouter = DOMNotificationRouter.create(16);
        this.domDataBroker = createDOMDataBroker();
        this.dataBroker = new BindingDOMDataBrokerAdapter(this.adapterContext, this.domDataBroker);
        this.notificationService = new BindingDOMNotificationServiceAdapter(this.adapterContext,
                this.domNotificationRouter);
        this.xmlNodeConverter = new XmlNodeConverter(this.effectiveModelContext);
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
    public Inference getRootInference() {
        return SchemaInferenceStack.of(effectiveModelContext).toInference();
    }

    @Override
    public AdapterContext getAdapterContext() {
        return this.adapterContext;
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

    private Map<LogicalDatastoreType, DOMStore> createDatastores() {
        return ImmutableMap.<LogicalDatastoreType, DOMStore>builder()
                .put(LogicalDatastoreType.OPERATIONAL, createOperationalDatastore())
                .put(LogicalDatastoreType.CONFIGURATION, createConfigurationDatastore()).build();
    }

    private DOMStore createConfigurationDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("CFG", LogicalDatastoreType.CONFIGURATION,
                getDataTreeChangeListenerExecutor(),
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE, false);
        return store;
    }

    private DOMStore createOperationalDatastore() {
        final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", getDataTreeChangeListenerExecutor());
        return store;
    }

    private AdapterContext createAdapterContext(Collection<YangModuleInfo> moduleInfos) {
        final YangParserFactory yangParserFactory = new DefaultYangParserFactory();
        ModuleInfoSnapshotResolver snapshotResolver
                = new ModuleInfoSnapshotResolver("netconf-simulator", yangParserFactory);
        snapshotResolver.registerModuleInfos(moduleInfos);
        ModuleInfoSnapshot moduleInfoSnapshot = snapshotResolver.takeSnapshot();

        final BindingRuntimeGenerator bindingRuntimeGenerator = new DefaultBindingRuntimeGenerator();
        final BindingRuntimeTypes bindingRuntimeTypes = bindingRuntimeGenerator
                .generateTypeMapping(moduleInfoSnapshot.getEffectiveModelContext());
        final DefaultBindingRuntimeContext bindingRuntimeContext
                = new DefaultBindingRuntimeContext(bindingRuntimeTypes, moduleInfoSnapshot);

        final BindingCodecContext bindingCodecContext = new BindingCodecContext(bindingRuntimeContext);
        return new ConstantAdapterContext(bindingCodecContext);
    }

}
