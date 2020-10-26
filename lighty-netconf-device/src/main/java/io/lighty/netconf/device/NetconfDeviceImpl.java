/*
 * Copyright (c) 2020 PANTHEON.tech s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */
package io.lighty.netconf.device;

import com.google.common.util.concurrent.FluentFuture;
import io.lighty.codecs.api.SerializationException;
import io.lighty.netconf.device.requests.RequestProcessor;
import io.lighty.netconf.device.requests.RpcHandlerImpl;
import io.lighty.netconf.device.requests.notification.NotificationPublishServiceImpl;
import io.lighty.netconf.device.utils.TimeoutUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.netconf.test.tool.config.Configuration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceImpl implements NetconfDevice {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceImpl.class);
    private static final long TIMEOUT_MILLIS = 30_000;

    private NetconfDeviceServices netconfDeviceServices;
    private NetconfDeviceSimulator netConfDeviceSimulator;
    private InputStream initialOperationalData;
    private InputStream initialConfigurationData;
    private boolean netconfMonitoringEnabled;

    public NetconfDeviceImpl(Collection<YangModuleInfo> moduleInfos, Configuration config,
            InputStream initialOperationalData, InputStream initialConfigurationData,
            Map<QName, RequestProcessor> requestProcessors, NotificationPublishServiceImpl creator,
            boolean netconfMonitoringEnabled) {
        if (creator != null) {
            config.setOperationsCreator(creator);
        }
        this.netconfDeviceServices = new NetconfDeviceServicesImpl(moduleInfos, creator);
        this.initialOperationalData = initialOperationalData;
        this.initialConfigurationData = initialConfigurationData;
        RpcHandlerImpl rpcHandler = new RpcHandlerImpl(netconfDeviceServices, requestProcessors);
        config.setRpcHandler(rpcHandler);
        this.netConfDeviceSimulator = new NetconfDeviceSimulator(config);
        this.netconfMonitoringEnabled = netconfMonitoringEnabled;
    }

    @Override
    public void start() {
        LOG.info("Starting Netconf device");
        if (initialOperationalData != null) {
            initDatastore(LogicalDatastoreType.OPERATIONAL, initialOperationalData);
        }
        if (initialConfigurationData != null) {
            initDatastore(LogicalDatastoreType.CONFIGURATION, initialConfigurationData);
        }
        netConfDeviceSimulator.start();
        if (netconfMonitoringEnabled) {
            try {
                prepareSchemasForNetconfMonitoring().get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                LOG.info("Netconf monitoring enabled successfully");
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                LOG.error("Could not prepare Schemas to expose through NETCONF Monitoring", e);
            }
        }
        LOG.info("Netconf device started");
    }

    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    private void initDatastore(LogicalDatastoreType datastoreType, InputStream initialData) {
        LOG.debug("Setting up initial state of {} datastore from XML", datastoreType);
        try (Reader reader = new InputStreamReader(initialData, Charset.defaultCharset())) {
            NormalizedNode<?, ?> initialDataBI = netconfDeviceServices.getXmlNodeConverter()
                    .deserialize(netconfDeviceServices.getRootSchemaNode(), reader);
            DOMDataTreeWriteTransaction writeTx = netconfDeviceServices.getDOMDataBroker().newWriteOnlyTransaction();
            writeTx.put(datastoreType, YangInstanceIdentifier.empty(), initialDataBI);
            writeTx.commit().get(TimeoutUtil.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            final String dataTreeString = NormalizedNodes.toStringTree(initialDataBI);
            LOG.trace("Initial {} datastore data: {}", datastoreType, dataTreeString);
        } catch (SerializationException | IOException | ExecutionException | InterruptedException | TimeoutException e) {
            String msg = "Unable to set initial state of " + datastoreType + " datastore from XML!";
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    @Override
    public NetconfDeviceServices getNetconfDeviceServices() {
        return netconfDeviceServices;
    }

    @Override
    public void close() throws Exception {
        LOG.info("shutting down Netconf device");
        netConfDeviceSimulator.close();
    }

    /**
     * Method creates schemas from device's schema context and
     * stores them in the netconf-state/schemas path in operational datastore.
     * netconfDeviceServices represents device services from which
     * the modules loaded in schema-context will be converted to schemas
     * stored in netconf-state/schemas
     * @return transaction commit information in FluentFuture
     */
    private FluentFuture<? extends CommitInfo> prepareSchemasForNetconfMonitoring() {
        HashMap<SchemaKey, Schema> mapSchemas = new HashMap<>();
        SchemaContext modelContext = netconfDeviceServices.getSchemaContext();
        Queue<Collection<? extends Module>> queueModulesCollections = new LinkedList<>();
        queueModulesCollections.add(modelContext.getModules());
        while (!queueModulesCollections.isEmpty()) {
            Collection<? extends Module> modules = queueModulesCollections.poll();
            for (Module module : modules) {
                Schema schema = createSchemaFromModule(module);
                if (!mapSchemas.containsKey(schema.key())) {
                    mapSchemas.put(schema.key(), schema);
                    if (!module.getSubmodules().isEmpty()) {
                        queueModulesCollections.add(module.getSubmodules());
                    }
                }
            }
        }
        WriteTransaction writeTx = netconfDeviceServices.getDataBroker().newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(NetconfState.class).child(Schemas.class).build(),
            new SchemasBuilder().setSchema(new ArrayList<>(mapSchemas.values())).build());
        return writeTx.commit();
    }

    /**
     * Creates and returns schema from given module.
     * @param module represents module from which schema will be created
     * @return schema created from module parameter
     */
    private Schema createSchemaFromModule(Module module) {
        return new SchemaBuilder()
            .setNamespace(new Uri(module.getNamespace().toString()))
            .setFormat(Yang.class)
            .setIdentifier(module.getName())
            .setVersion(module.getRevision().map(Revision::toString).orElse(""))
            .setLocation(Collections.singletonList(new Schema.Location(Schema.Location.Enumeration.NETCONF)))
            .build();
    }

}
